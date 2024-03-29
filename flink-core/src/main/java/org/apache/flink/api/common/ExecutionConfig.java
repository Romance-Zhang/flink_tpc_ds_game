/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.configuration.MetricOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.util.Preconditions;

import com.esotericsoftware.kryo.Serializer;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * A config to define the behavior of the program execution. It allows to define (among other
 * options) the following settings:
 *
 * <ul>
 *     <li>The default parallelism of the program, i.e., how many parallel tasks to use for
 *         all functions that do not define a specific value directly.</li>
 *     <li>The number of retries in the case of failed executions.</li>
 *     <li>The delay between execution retries.</li>
 *     <li>The {@link ExecutionMode} of the program: Batch or Pipelined.
 *         The default execution mode is {@link ExecutionMode#PIPELINED}</li>
 *     <li>Enabling or disabling the "closure cleaner". The closure cleaner pre-processes
 *         the implementations of functions. In case they are (anonymous) inner classes,
 *         it removes unused references to the enclosing class to fix certain serialization-related
 *         problems and to reduce the size of the closure.</li>
 *     <li>The config allows to register types and serializers to increase the efficiency of
 *         handling <i>generic types</i> and <i>POJOs</i>. This is usually only needed
 *         when the functions return not only the types declared in their signature, but
 *         also subclasses of those types.</li>
 * </ul>
 */
@Public
public class ExecutionConfig implements Serializable, Archiveable<ArchivedExecutionConfig> {

	private static final long serialVersionUID = 1L;

	/**
	 * The constant to use for the parallelism, if the system should use the number
	 * of currently available slots.
	 */
	@Deprecated
	public static final int PARALLELISM_AUTO_MAX = Integer.MAX_VALUE;

	/**
	 * The flag value indicating use of the default parallelism. This value can
	 * be used to reset the parallelism back to the default state.
	 */
	public static final int PARALLELISM_DEFAULT = -1;

	/**
	 * The flag value indicating an unknown or unset parallelism. This value is
	 * not a valid parallelism and indicates that the parallelism should remain
	 * unchanged.
	 */
	public static final int PARALLELISM_UNKNOWN = -2;

	private static final long DEFAULT_RESTART_DELAY = 10000L;

	// --------------------------------------------------------------------------------------------

	/** Defines how data exchange happens - batch or pipelined */
	private ExecutionMode executionMode = ExecutionMode.PIPELINED;

	private ClosureCleanerLevel closureCleanerLevel = ClosureCleanerLevel.RECURSIVE;

	private int parallelism = PARALLELISM_DEFAULT;

	/**
	 * The program wide maximum parallelism used for operators which haven't specified a maximum
	 * parallelism. The maximum parallelism specifies the upper limit for dynamic scaling and the
	 * number of key groups used for partitioned state.
	 */
	private int maxParallelism = -1;

	/**
	 * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
	 */
	@Deprecated
	private int numberOfExecutionRetries = -1;

	private boolean forceKryo = false;

	/** Flag to indicate whether generic types (through Kryo) are supported */
	private boolean disableGenericTypes = false;

	private boolean enableAutoGeneratedUids = true;

	private boolean objectReuse = false;

	private boolean autoTypeRegistrationEnabled = true;

	private boolean forceAvro = false;

	private CodeAnalysisMode codeAnalysisMode = CodeAnalysisMode.DISABLE;

	/** If set to true, progress updates are printed to System.out during execution */
	private boolean printProgressDuringExecution = true;

	private long autoWatermarkInterval = 0;

	/**
	 * Interval in milliseconds for sending latency tracking marks from the sources to the sinks.
	 */
	private long latencyTrackingInterval = MetricOptions.LATENCY_INTERVAL.defaultValue();

	private boolean isLatencyTrackingConfigured = false;

	/**
	 * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
	 */
	@Deprecated
	private long executionRetryDelay = DEFAULT_RESTART_DELAY;

	private RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration =
		new RestartStrategies.FallbackRestartStrategyConfiguration();
	
	private long taskCancellationIntervalMillis = -1;

	/**
	 * Timeout after which an ongoing task cancellation will lead to a fatal
	 * TaskManager error, usually killing the JVM.
	 */
	private long taskCancellationTimeoutMillis = -1;

	/** This flag defines if we use compression for the state snapshot data or not. Default: false */
	private boolean useSnapshotCompression = false;

	/**
	 * @deprecated Should no longer be used because we would not support to let task directly fail on checkpoint error.
	 */
	@Deprecated
	private boolean failTaskOnCheckpointError = true;

	/** The default input dependency constraint to schedule tasks. */
	private InputDependencyConstraint defaultInputDependencyConstraint = InputDependencyConstraint.ANY;

	// ------------------------------- User code values --------------------------------------------

	private GlobalJobParameters globalJobParameters;

	// Serializers and types registered with Kryo and the PojoSerializer
	// we store them in linked maps/sets to ensure they are registered in order in all kryo instances.

	private LinkedHashMap<Class<?>, SerializableSerializer<?>> registeredTypesWithKryoSerializers = new LinkedHashMap<>();

	private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> registeredTypesWithKryoSerializerClasses = new LinkedHashMap<>();

	private LinkedHashMap<Class<?>, SerializableSerializer<?>> defaultKryoSerializers = new LinkedHashMap<>();

	private LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> defaultKryoSerializerClasses = new LinkedHashMap<>();

	private LinkedHashSet<Class<?>> registeredKryoTypes = new LinkedHashSet<>();

	private LinkedHashSet<Class<?>> registeredPojoTypes = new LinkedHashSet<>();

	// --------------------------------------------------------------------------------------------

	/**
	 * Enables the ClosureCleaner. This analyzes user code functions and sets fields to null
	 * that are not used. This will in most cases make closures or anonymous inner classes
	 * serializable that where not serializable due to some Scala or Java implementation artifact.
	 * User code must be serializable because it needs to be sent to worker nodes.
	 */
	public ExecutionConfig enableClosureCleaner() {
		this.closureCleanerLevel = ClosureCleanerLevel.RECURSIVE;
		return this;
	}

	/**
	 * Disables the ClosureCleaner.
	 *
	 * @see #enableClosureCleaner()
	 */
	public ExecutionConfig disableClosureCleaner() {
		this.closureCleanerLevel = ClosureCleanerLevel.NONE;
		return this;
	}

	/**
	 * Returns whether the ClosureCleaner is enabled.
	 *
	 * @see #enableClosureCleaner()
	 */
	public boolean isClosureCleanerEnabled() {
		return !(closureCleanerLevel == ClosureCleanerLevel.NONE);
	}

	/**
	 * Configures the closure cleaner. Please see {@link ClosureCleanerLevel} for details on the
	 * different settings.
	 */
	public ExecutionConfig setClosureCleanerLevel(ClosureCleanerLevel level) {
		this.closureCleanerLevel = level;
		return this;
	}

	/**
	 * Returns the configured {@link ClosureCleanerLevel}.
	 */
	public ClosureCleanerLevel getClosureCleanerLevel() {
		return closureCleanerLevel;
	}

	/**
	 * Sets the interval of the automatic watermark emission. Watermarks are used throughout
	 * the streaming system to keep track of the progress of time. They are used, for example,
	 * for time based windowing.
	 *
	 * @param interval The interval between watermarks in milliseconds.
	 */
	@PublicEvolving
	public ExecutionConfig setAutoWatermarkInterval(long interval) {
		Preconditions.checkArgument(interval >= 0, "Auto watermark interval must not be negative.");
		this.autoWatermarkInterval = interval;
		return this;
	}

	/**
	 * Returns the interval of the automatic watermark emission.
	 *
	 * @see #setAutoWatermarkInterval(long)
	 */
	@PublicEvolving
	public long getAutoWatermarkInterval()  {
		return this.autoWatermarkInterval;
	}

	/**
	 * Interval for sending latency tracking marks from the sources to the sinks.
	 * Flink will send latency tracking marks from the sources at the specified interval.
	 *
	 * Setting a tracking interval <= 0 disables the latency tracking.
	 *
	 * @param interval Interval in milliseconds.
	 */
	@PublicEvolving
	public ExecutionConfig setLatencyTrackingInterval(long interval) {
		this.latencyTrackingInterval = interval;
		this.isLatencyTrackingConfigured = true;
		return this;
	}

	/**
	 * Returns the latency tracking interval.
	 * @return The latency tracking interval in milliseconds
	 */
	@PublicEvolving
	public long getLatencyTrackingInterval() {
		return latencyTrackingInterval;
	}

	/**
	 * @deprecated will be removed in a future version
	 */
	@PublicEvolving
	@Deprecated
	public boolean isLatencyTrackingEnabled() {
		return isLatencyTrackingConfigured && latencyTrackingInterval > 0;
	}

	@Internal
	public boolean isLatencyTrackingConfigured() {
		return isLatencyTrackingConfigured;
	}

	/**
	 * Gets the parallelism with which operation are executed by default. Operations can
	 * individually override this value to use a specific parallelism.
	 *
	 * Other operations may need to run with a different parallelism - for example calling
	 * a reduce operation over the entire data set will involve an operation that runs
	 * with a parallelism of one (the final reduce to the single result value).
	 *
	 * @return The parallelism used by operations, unless they override that value. This method
	 *         returns {@link #PARALLELISM_DEFAULT} if the environment's default parallelism
	 *         should be used.
	 */
	public int getParallelism() {
		return parallelism;
	}

	/**
	 * Sets the parallelism for operations executed through this environment.
	 * Setting a parallelism of x here will cause all operators (such as join, map, reduce) to run with
	 * x parallel instances.
	 * <p>
	 * This method overrides the default parallelism for this environment.
	 * The local execution environment uses by default a value equal to the number of hardware
	 * contexts (CPU cores / threads). When executing the program via the command line client
	 * from a JAR file, the default parallelism is the one configured for that setup.
	 *
	 * @param parallelism The parallelism to use
	 */
	public ExecutionConfig setParallelism(int parallelism) {
		if (parallelism != PARALLELISM_UNKNOWN) {
			if (parallelism < 1 && parallelism != PARALLELISM_DEFAULT) {
				throw new IllegalArgumentException(
					"Parallelism must be at least one, or ExecutionConfig.PARALLELISM_DEFAULT (use system default).");
			}
			this.parallelism = parallelism;
		}
		return this;
	}

	/**
	 * Gets the maximum degree of parallelism defined for the program.
	 *
	 * The maximum degree of parallelism specifies the upper limit for dynamic scaling. It also
	 * defines the number of key groups used for partitioned state.
	 *
	 * @return Maximum degree of parallelism
	 */
	@PublicEvolving
	public int getMaxParallelism() {
		return maxParallelism;
	}

	/**
	 * Sets the maximum degree of parallelism defined for the program.
	 *
	 * The maximum degree of parallelism specifies the upper limit for dynamic scaling. It also
	 * defines the number of key groups used for partitioned state.
	 *
	 * @param maxParallelism Maximum degree of parallelism to be used for the program.
	 */
	@PublicEvolving
	public void setMaxParallelism(int maxParallelism) {
		checkArgument(maxParallelism > 0, "The maximum parallelism must be greater than 0.");
		this.maxParallelism = maxParallelism;
	}

	/**
	 * Gets the interval (in milliseconds) between consecutive attempts to cancel a running task.
	 */
	public long getTaskCancellationInterval() {
		return this.taskCancellationIntervalMillis;
	}

	/**
	 * Sets the configuration parameter specifying the interval (in milliseconds)
	 * between consecutive attempts to cancel a running task.
	 * @param interval the interval (in milliseconds).
	 */
	public ExecutionConfig setTaskCancellationInterval(long interval) {
		this.taskCancellationIntervalMillis = interval;
		return this;
	}

	/**
	 * Returns the timeout (in milliseconds) after which an ongoing task
	 * cancellation leads to a fatal TaskManager error.
	 *
	 * <p>The value <code>0</code> means that the timeout is disabled. In
	 * this case a stuck cancellation will not lead to a fatal error.
	 */
	@PublicEvolving
	public long getTaskCancellationTimeout() {
		return this.taskCancellationTimeoutMillis;
	}

	/**
	 * Sets the timeout (in milliseconds) after which an ongoing task cancellation
	 * is considered failed, leading to a fatal TaskManager error.
	 *
	 * <p>The cluster default is configured via {@link TaskManagerOptions#TASK_CANCELLATION_TIMEOUT}.
	 *
	 * <p>The value <code>0</code> disables the timeout. In this case a stuck
	 * cancellation will not lead to a fatal error.
	 *
	 * @param timeout The task cancellation timeout (in milliseconds).
	 */
	@PublicEvolving
	public ExecutionConfig setTaskCancellationTimeout(long timeout) {
		checkArgument(timeout >= 0, "Timeout needs to be >= 0.");
		this.taskCancellationTimeoutMillis = timeout;
		return this;
	}

	/**
	 * Sets the restart strategy to be used for recovery.
	 *
	 * <pre>{@code
	 * ExecutionConfig config = env.getConfig();
	 *
	 * config.setRestartStrategy(RestartStrategies.fixedDelayRestart(
	 * 	10,  // number of retries
	 * 	1000 // delay between retries));
	 * }</pre>
	 *
	 * @param restartStrategyConfiguration Configuration defining the restart strategy to use
	 */
	@PublicEvolving
	public void setRestartStrategy(RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration) {
		this.restartStrategyConfiguration = Preconditions.checkNotNull(restartStrategyConfiguration);
	}

	/**
	 * Returns the restart strategy which has been set for the current job.
	 *
	 * @return The specified restart configuration
	 */
	@PublicEvolving
	@SuppressWarnings("deprecation")
	public RestartStrategies.RestartStrategyConfiguration getRestartStrategy() {
		if (restartStrategyConfiguration instanceof RestartStrategies.FallbackRestartStrategyConfiguration) {
			// support the old API calls by creating a restart strategy from them
			if (getNumberOfExecutionRetries() > 0 && getExecutionRetryDelay() >= 0) {
				return RestartStrategies.fixedDelayRestart(getNumberOfExecutionRetries(), getExecutionRetryDelay());
			} else if (getNumberOfExecutionRetries() == 0) {
				return RestartStrategies.noRestart();
			} else {
				return restartStrategyConfiguration;
			}
		} else {
			return restartStrategyConfiguration;
		}
	}

	/**
	 * Gets the number of times the system will try to re-execute failed tasks. A value
	 * of {@code -1} indicates that the system default value (as defined in the configuration)
	 * should be used.
	 *
	 * @return The number of times the system will try to re-execute failed tasks.
	 *
	 * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
	 */
	@Deprecated
	public int getNumberOfExecutionRetries() {
		return numberOfExecutionRetries;
	}

	/**
	 * Returns the delay between execution retries.
	 *
	 * @return The delay between successive execution retries in milliseconds.
	 *
	 * @deprecated Should no longer be used because it is subsumed by RestartStrategyConfiguration
	 */
	@Deprecated
	public long getExecutionRetryDelay() {
		return executionRetryDelay;
	}

	/**
	 * Sets the number of times that failed tasks are re-executed. A value of zero
	 * effectively disables fault tolerance. A value of {@code -1} indicates that the system
	 * default value (as defined in the configuration) should be used.
	 *
	 * @param numberOfExecutionRetries The number of times the system will try to re-execute failed tasks.
	 *
	 * @return The current execution configuration
	 *
	 * @deprecated This method will be replaced by {@link #setRestartStrategy}. The
	 * {@link RestartStrategies.FixedDelayRestartStrategyConfiguration} contains the number of
	 * execution retries.
	 */
	@Deprecated
	public ExecutionConfig setNumberOfExecutionRetries(int numberOfExecutionRetries) {
		if (numberOfExecutionRetries < -1) {
			throw new IllegalArgumentException(
				"The number of execution retries must be non-negative, or -1 (use system default)");
		}
		this.numberOfExecutionRetries = numberOfExecutionRetries;
		return this;
	}

	/**
	 * Sets the delay between executions.
	 *
	 * @param executionRetryDelay The number of milliseconds the system will wait to retry.
	 *
	 * @return The current execution configuration
	 *
	 * @deprecated This method will be replaced by {@link #setRestartStrategy}. The
	 * {@link RestartStrategies.FixedDelayRestartStrategyConfiguration} contains the delay between
	 * successive execution attempts.
	 */
	@Deprecated
	public ExecutionConfig setExecutionRetryDelay(long executionRetryDelay) {
		if (executionRetryDelay < 0 ) {
			throw new IllegalArgumentException(
				"The delay between retries must be non-negative.");
		}
		this.executionRetryDelay = executionRetryDelay;
		return this;
	}

	/**
	 * Sets the execution mode to execute the program. The execution mode defines whether
	 * data exchanges are performed in a batch or on a pipelined manner.
	 *
	 * The default execution mode is {@link ExecutionMode#PIPELINED}.
	 *
	 * @param executionMode The execution mode to use.
	 */
	public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

	/**
	 * Gets the execution mode used to execute the program. The execution mode defines whether
	 * data exchanges are performed in a batch or on a pipelined manner.
	 *
	 * The default execution mode is {@link ExecutionMode#PIPELINED}.
	 *
	 * @return The execution mode for the program.
	 */
	public ExecutionMode getExecutionMode() {
		return executionMode;
	}

	/**
	 * Sets the default input dependency constraint for vertex scheduling. It indicates when a task
	 * should be scheduled considering its inputs status.
	 *
	 * <p>The default constraint is {@link InputDependencyConstraint#ANY}.
	 *
	 * @param inputDependencyConstraint The input dependency constraint.
	 */
	@PublicEvolving
	public void setDefaultInputDependencyConstraint(InputDependencyConstraint inputDependencyConstraint) {
		this.defaultInputDependencyConstraint = inputDependencyConstraint;
	}

	/**
	 * Gets the default input dependency constraint for vertex scheduling. It indicates when a task
	 * should be scheduled considering its inputs status.
	 *
	 * <p>The default constraint is {@link InputDependencyConstraint#ANY}.
	 *
	 * @return The input dependency constraint of this job.
	 */
	@PublicEvolving
	public InputDependencyConstraint getDefaultInputDependencyConstraint() {
		return defaultInputDependencyConstraint;
	}

	/**
	 * Force TypeExtractor to use Kryo serializer for POJOS even though we could analyze as POJO.
	 * In some cases this might be preferable. For example, when using interfaces
	 * with subclasses that cannot be analyzed as POJO.
	 */
	public void enableForceKryo() {
		forceKryo = true;
	}

	/**
	 * Disable use of Kryo serializer for all POJOs.
	 */
	public void disableForceKryo() {
		forceKryo = false;
	}

	public boolean isForceKryoEnabled() {
		return forceKryo;
	}

	/**
	 * Enables the use generic types which are serialized via Kryo.
	 * 
	 * <p>Generic types are enabled by default.
	 *
	 * @see #disableGenericTypes()
	 */
	public void enableGenericTypes() {
		disableGenericTypes = false;
	}

	/**
	 * Disables the use of generic types (types that would be serialized via Kryo). If this option
	 * is used, Flink will throw an {@code UnsupportedOperationException} whenever it encounters
	 * a data type that would go through Kryo for serialization.
	 *
	 * <p>Disabling generic types can be helpful to eagerly find and eliminate the use of types
	 * that would go through Kryo serialization during runtime. Rather than checking types
	 * individually, using this option will throw exceptions eagerly in the places where generic
	 * types are used.
	 * 
	 * <p><b>Important:</b> We recommend to use this option only during development and pre-production
	 * phases, not during actual production use. The application program and/or the input data may be
	 * such that new, previously unseen, types occur at some point. In that case, setting this option
	 * would cause the program to fail.
	 * 
	 * @see #enableGenericTypes()
	 */
	public void disableGenericTypes() {
		disableGenericTypes = true;
	}

	/**
	 * Checks whether generic types are supported. Generic types are types that go through Kryo during
	 * serialization.
	 * 
	 * <p>Generic types are enabled by default.
	 * 
	 * @see #enableGenericTypes()
	 * @see #disableGenericTypes()
	 */
	public boolean hasGenericTypesDisabled() {
		return disableGenericTypes;
	}

	/**
	 * Enables the Flink runtime to auto-generate UID's for operators.
	 *
	 * @see #disableAutoGeneratedUIDs()
	 */
	public void enableAutoGeneratedUIDs() {
		enableAutoGeneratedUids = true;
	}

	/**
	 * Disables auto-generated UIDs. Forces users to manually specify UIDs
	 * on DataStream applications.
	 *
	 * <p>It is highly recommended that users specify UIDs before deploying to
	 * production since they are used to match state in savepoints to operators
	 * in a job. Because auto-generated ID's are likely to change when modifying
	 * a job, specifying custom IDs allow an application to evolve overtime
	 * without discarding state.
	 */
	public void  disableAutoGeneratedUIDs() {
		enableAutoGeneratedUids = false;
	}

	/**
	 * Checks whether auto generated UIDs are supported.
	 *
	 * <p>Auto generated UIDs are enabled by default.
	 *
	 * @see #enableAutoGeneratedUIDs()
	 * @see #disableAutoGeneratedUIDs()
	 */
	public boolean hasAutoGeneratedUIDsEnabled() {
		return enableAutoGeneratedUids;
	}

	/**
	 * Forces Flink to use the Apache Avro serializer for POJOs.
	 *
	 * <b>Important:</b> Make sure to include the <i>flink-avro</i> module.
	 */
	public void enableForceAvro() {
		forceAvro = true;
	}

	/**
	 * Disables the Apache Avro serializer as the forced serializer for POJOs.
	 */
	public void disableForceAvro() {
		forceAvro = false;
	}

	/**
	 * Returns whether the Apache Avro is the default serializer for POJOs.
	 */
	public boolean isForceAvroEnabled() {
		return forceAvro;
	}

	/**
	 * Enables reusing objects that Flink internally uses for deserialization and passing
	 * data to user-code functions. Keep in mind that this can lead to bugs when the
	 * user-code function of an operation is not aware of this behaviour.
	 */
	public ExecutionConfig enableObjectReuse() {
		objectReuse = true;
		return this;
	}

	/**
	 * Disables reusing objects that Flink internally uses for deserialization and passing
	 * data to user-code functions. @see #enableObjectReuse()
	 */
	public ExecutionConfig disableObjectReuse() {
		objectReuse = false;
		return this;
	}

	/**
	 * Returns whether object reuse has been enabled or disabled. @see #enableObjectReuse()
	 */
	public boolean isObjectReuseEnabled() {
		return objectReuse;
	}
	
	/**
	 * @deprecated The code analysis code has been removed and this method has no effect.
	 */
	@PublicEvolving
	@Deprecated
	public void setCodeAnalysisMode(CodeAnalysisMode codeAnalysisMode) {
	}
	
	/**
	 * @deprecated The code analysis code has been removed and this method does not return anything interesting.
	 */
	@PublicEvolving
	@Deprecated
	public CodeAnalysisMode getCodeAnalysisMode() {
		return codeAnalysisMode;
	}

	/**
	 * Enables the printing of progress update messages to {@code System.out}
	 * 
	 * @return The ExecutionConfig object, to allow for function chaining.
	 */
	public ExecutionConfig enableSysoutLogging() {
		this.printProgressDuringExecution = true;
		return this;
	}

	/**
	 * Disables the printing of progress update messages to {@code System.out}
	 *
	 * @return The ExecutionConfig object, to allow for function chaining.
	 */
	public ExecutionConfig disableSysoutLogging() {
		this.printProgressDuringExecution = false;
		return this;
	}

	/**
	 * Gets whether progress update messages should be printed to {@code System.out}
	 * 
	 * @return True, if progress update messages should be printed, false otherwise.
	 */
	public boolean isSysoutLoggingEnabled() {
		return this.printProgressDuringExecution;
	}

	public GlobalJobParameters getGlobalJobParameters() {
		return globalJobParameters;
	}

	/**
	 * Register a custom, serializable user configuration object.
	 * @param globalJobParameters Custom user configuration object
	 */
	public void setGlobalJobParameters(GlobalJobParameters globalJobParameters) {
		this.globalJobParameters = globalJobParameters;
	}

	// --------------------------------------------------------------------------------------------
	//  Registry for types and serializers
	// --------------------------------------------------------------------------------------------

	/**
	 * Adds a new Kryo default serializer to the Runtime.
	 *
	 * Note that the serializer instance must be serializable (as defined by java.io.Serializable),
	 * because it may be distributed to the worker nodes by java serialization.
	 *
	 * @param type The class of the types serialized with the given serializer.
	 * @param serializer The serializer to use.
	 */
	public <T extends Serializer<?> & Serializable>void addDefaultKryoSerializer(Class<?> type, T serializer) {
		if (type == null || serializer == null) {
			throw new NullPointerException("Cannot register null class or serializer.");
		}

		defaultKryoSerializers.put(type, new SerializableSerializer<>(serializer));
	}

	/**
	 * Adds a new Kryo default serializer to the Runtime.
	 *
	 * @param type The class of the types serialized with the given serializer.
	 * @param serializerClass The class of the serializer to use.
	 */
	public void addDefaultKryoSerializer(Class<?> type, Class<? extends Serializer<?>> serializerClass) {
		if (type == null || serializerClass == null) {
			throw new NullPointerException("Cannot register null class or serializer.");
		}
		defaultKryoSerializerClasses.put(type, serializerClass);
	}

	/**
	 * Registers the given type with a Kryo Serializer.
	 *
	 * Note that the serializer instance must be serializable (as defined by java.io.Serializable),
	 * because it may be distributed to the worker nodes by java serialization.
	 *
	 * @param type The class of the types serialized with the given serializer.
	 * @param serializer The serializer to use.
	 */
	public <T extends Serializer<?> & Serializable>void registerTypeWithKryoSerializer(Class<?> type, T serializer) {
		if (type == null || serializer == null) {
			throw new NullPointerException("Cannot register null class or serializer.");
		}

		registeredTypesWithKryoSerializers.put(type, new SerializableSerializer<>(serializer));
	}

	/**
	 * Registers the given Serializer via its class as a serializer for the given type at the KryoSerializer
	 *
	 * @param type The class of the types serialized with the given serializer.
	 * @param serializerClass The class of the serializer to use.
	 */
	@SuppressWarnings("rawtypes")
	public void registerTypeWithKryoSerializer(Class<?> type, Class<? extends Serializer> serializerClass) {
		if (type == null || serializerClass == null) {
			throw new NullPointerException("Cannot register null class or serializer.");
		}

		@SuppressWarnings("unchecked")
		Class<? extends Serializer<?>> castedSerializerClass = (Class<? extends Serializer<?>>) serializerClass;
		registeredTypesWithKryoSerializerClasses.put(type, castedSerializerClass);
	}

	/**
	 * Registers the given type with the serialization stack. If the type is eventually
	 * serialized as a POJO, then the type is registered with the POJO serializer. If the
	 * type ends up being serialized with Kryo, then it will be registered at Kryo to make
	 * sure that only tags are written.
	 *
	 * @param type The class of the type to register.
	 */
	public void registerPojoType(Class<?> type) {
		if (type == null) {
			throw new NullPointerException("Cannot register null type class.");
		}
		if (!registeredPojoTypes.contains(type)) {
			registeredPojoTypes.add(type);
		}
	}

	/**
	 * Registers the given type with the serialization stack. If the type is eventually
	 * serialized as a POJO, then the type is registered with the POJO serializer. If the
	 * type ends up being serialized with Kryo, then it will be registered at Kryo to make
	 * sure that only tags are written.
	 *
	 * @param type The class of the type to register.
	 */
	public void registerKryoType(Class<?> type) {
		if (type == null) {
			throw new NullPointerException("Cannot register null type class.");
		}
		registeredKryoTypes.add(type);
	}

	/**
	 * Returns the registered types with Kryo Serializers.
	 */
	public LinkedHashMap<Class<?>, SerializableSerializer<?>> getRegisteredTypesWithKryoSerializers() {
		return registeredTypesWithKryoSerializers;
	}

	/**
	 * Returns the registered types with their Kryo Serializer classes.
	 */
	public LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> getRegisteredTypesWithKryoSerializerClasses() {
		return registeredTypesWithKryoSerializerClasses;
	}


	/**
	 * Returns the registered default Kryo Serializers.
	 */
	public LinkedHashMap<Class<?>, SerializableSerializer<?>> getDefaultKryoSerializers() {
		return defaultKryoSerializers;
	}

	/**
	 * Returns the registered default Kryo Serializer classes.
	 */
	public LinkedHashMap<Class<?>, Class<? extends Serializer<?>>> getDefaultKryoSerializerClasses() {
		return defaultKryoSerializerClasses;
	}

	/**
	 * Returns the registered Kryo types.
	 */
	public LinkedHashSet<Class<?>> getRegisteredKryoTypes() {
		if (isForceKryoEnabled()) {
			// if we force kryo, we must also return all the types that
			// were previously only registered as POJO
			LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
			result.addAll(registeredKryoTypes);
			for(Class<?> t : registeredPojoTypes) {
				if (!result.contains(t)) {
					result.add(t);
				}
			}
			return result;
		} else {
			return registeredKryoTypes;
		}
	}

	/**
	 * Returns the registered POJO types.
	 */
	public LinkedHashSet<Class<?>> getRegisteredPojoTypes() {
		return registeredPojoTypes;
	}


	public boolean isAutoTypeRegistrationDisabled() {
		return !autoTypeRegistrationEnabled;
	}

	/**
	 * Control whether Flink is automatically registering all types in the user programs with
	 * Kryo.
	 *
	 */
	public void disableAutoTypeRegistration() {
		this.autoTypeRegistrationEnabled = false;
	}

	public boolean isUseSnapshotCompression() {
		return useSnapshotCompression;
	}

	public void setUseSnapshotCompression(boolean useSnapshotCompression) {
		this.useSnapshotCompression = useSnapshotCompression;
	}

	/**
	 * @deprecated This method takes no effect since we would not forward the configuration from the checkpoint config
	 * to the task, and we have not supported task to fail on checkpoint error.
	 * Please use CheckpointConfig.getTolerableCheckpointFailureNumber() to know the behavior on checkpoint errors.
	 */
	@Deprecated
	@Internal
	public boolean isFailTaskOnCheckpointError() {
		return failTaskOnCheckpointError;
	}

	/**
	 * @deprecated This method takes no effect since we would not forward the configuration from the checkpoint config
	 * to the task, and we have not supported task to fail on checkpoint error.
	 * Please use CheckpointConfig.setTolerableCheckpointFailureNumber(int) to determine the behavior on checkpoint errors.
	 */
	@Deprecated
	@Internal
	public void setFailTaskOnCheckpointError(boolean failTaskOnCheckpointError) {
		this.failTaskOnCheckpointError = failTaskOnCheckpointError;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExecutionConfig) {
			ExecutionConfig other = (ExecutionConfig) obj;

			return other.canEqual(this) &&
				Objects.equals(executionMode, other.executionMode) &&
				closureCleanerLevel == other.closureCleanerLevel &&
				parallelism == other.parallelism &&
				((restartStrategyConfiguration == null && other.restartStrategyConfiguration == null) ||
					(null != restartStrategyConfiguration && restartStrategyConfiguration.equals(other.restartStrategyConfiguration))) &&
				forceKryo == other.forceKryo &&
				disableGenericTypes == other.disableGenericTypes &&
				objectReuse == other.objectReuse &&
				autoTypeRegistrationEnabled == other.autoTypeRegistrationEnabled &&
				forceAvro == other.forceAvro &&
				Objects.equals(codeAnalysisMode, other.codeAnalysisMode) &&
				printProgressDuringExecution == other.printProgressDuringExecution &&
				Objects.equals(globalJobParameters, other.globalJobParameters) &&
				autoWatermarkInterval == other.autoWatermarkInterval &&
				registeredTypesWithKryoSerializerClasses.equals(other.registeredTypesWithKryoSerializerClasses) &&
				defaultKryoSerializerClasses.equals(other.defaultKryoSerializerClasses) &&
				registeredKryoTypes.equals(other.registeredKryoTypes) &&
				registeredPojoTypes.equals(other.registeredPojoTypes) &&
				taskCancellationIntervalMillis == other.taskCancellationIntervalMillis &&
				useSnapshotCompression == other.useSnapshotCompression &&
				defaultInputDependencyConstraint == other.defaultInputDependencyConstraint;

		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			executionMode,
				closureCleanerLevel,
			parallelism,
			restartStrategyConfiguration,
			forceKryo,
			disableGenericTypes,
			objectReuse,
			autoTypeRegistrationEnabled,
			forceAvro,
			codeAnalysisMode,
			printProgressDuringExecution,
			globalJobParameters,
			autoWatermarkInterval,
			registeredTypesWithKryoSerializerClasses,
			defaultKryoSerializerClasses,
			registeredKryoTypes,
			registeredPojoTypes,
			taskCancellationIntervalMillis,
			useSnapshotCompression,
			defaultInputDependencyConstraint);
	}

	public boolean canEqual(Object obj) {
		return obj instanceof ExecutionConfig;
	}
	
	@Override
	@Internal
	public ArchivedExecutionConfig archive() {
		return new ArchivedExecutionConfig(this);
	}


	// ------------------------------ Utilities  ----------------------------------

	public static class SerializableSerializer<T extends Serializer<?> & Serializable> implements Serializable {
		private static final long serialVersionUID = 4687893502781067189L;

		private T serializer;

		public SerializableSerializer(T serializer) {
			this.serializer = serializer;
		}

		public T getSerializer() {
			return serializer;
		}
	}

	/**
	 * Abstract class for a custom user configuration object registered at the execution config.
	 *
	 * This user config is accessible at runtime through
	 * getRuntimeContext().getExecutionConfig().GlobalJobParameters()
	 */
	public static class GlobalJobParameters implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * Convert UserConfig into a {@code Map<String, String>} representation.
		 * This can be used by the runtime, for example for presenting the user config in the web frontend.
		 *
		 * @return Key/Value representation of the UserConfig
		 */
		public Map<String, String> toMap() {
			return Collections.emptyMap();
		}
	}

	/**
	 * Configuration settings for the closure cleaner.
	 */
	public enum ClosureCleanerLevel {
		/**
		 * Disable the closure cleaner completely.
		 */
		NONE,

		/**
		 * Clean only the top-level class without recursing into fields.
		 */
		TOP_LEVEL,

		/**
		 * Clean all the fields recursively.
		 */
		RECURSIVE
	}
}
