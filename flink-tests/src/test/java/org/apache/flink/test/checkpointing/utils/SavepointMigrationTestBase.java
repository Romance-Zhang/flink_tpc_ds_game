/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.test.checkpointing.utils;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobSubmissionResult;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.runtime.checkpoint.savepoint.SavepointSerializers;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.flink.test.util.TestBaseUtils;
import org.apache.flink.util.OptionalFailure;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.FiniteDuration;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotEquals;

/**
 * Test savepoint migration.
 */
public abstract class SavepointMigrationTestBase extends TestBaseUtils {

	@BeforeClass
	public static void before() {
		SavepointSerializers.setFailWhenLegacyStateDetected(false);
	}

	@ClassRule
	public static final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

	@Rule
	public final MiniClusterWithClientResource miniClusterResource;

	private static final Logger LOG = LoggerFactory.getLogger(SavepointMigrationTestBase.class);
	private static final Deadline DEADLINE = new FiniteDuration(5, TimeUnit.MINUTES).fromNow();
	protected static final int DEFAULT_PARALLELISM = 4;

	protected static String getResourceFilename(String filename) {
		ClassLoader cl = SavepointMigrationTestBase.class.getClassLoader();
		URL resource = cl.getResource(filename);
		if (resource == null) {
			throw new NullPointerException("Missing snapshot resource.");
		}
		return resource.getFile();
	}

	protected SavepointMigrationTestBase() throws Exception {
		miniClusterResource = new MiniClusterWithClientResource(
			new MiniClusterResourceConfiguration.Builder()
				.setConfiguration(getConfiguration())
				.setNumberTaskManagers(1)
				.setNumberSlotsPerTaskManager(DEFAULT_PARALLELISM)
				.build());
	}

	private Configuration getConfiguration() throws Exception {
		// Flink configuration
		final Configuration config = new Configuration();

		config.setInteger(ConfigConstants.LOCAL_NUMBER_TASK_MANAGER, 1);
		config.setInteger(TaskManagerOptions.NUM_TASK_SLOTS, DEFAULT_PARALLELISM);

		UUID id = UUID.randomUUID();
		final File checkpointDir = TEMP_FOLDER.newFolder("checkpoints_" + id).getAbsoluteFile();
		final File savepointDir = TEMP_FOLDER.newFolder("savepoints_" + id).getAbsoluteFile();

		if (!checkpointDir.exists() || !savepointDir.exists()) {
			throw new Exception("Test setup failed: failed to create (temporary) directories.");
		}

		LOG.info("Created temporary checkpoint directory: " + checkpointDir + ".");
		LOG.info("Created savepoint directory: " + savepointDir + ".");

		config.setString(CheckpointingOptions.STATE_BACKEND, "memory");
		config.setString(CheckpointingOptions.CHECKPOINTS_DIRECTORY, checkpointDir.toURI().toString());
		config.setInteger(CheckpointingOptions.FS_SMALL_FILE_THRESHOLD, 0);
		config.setString(CheckpointingOptions.SAVEPOINT_DIRECTORY, savepointDir.toURI().toString());

		return config;
	}

	@SafeVarargs
	protected final void executeAndSavepoint(
			StreamExecutionEnvironment env,
			String savepointPath,
			Tuple2<String, Integer>... expectedAccumulators) throws Exception {

		ClusterClient<?> client = miniClusterResource.getClusterClient();
		client.setDetached(true);

		// Submit the job
		JobGraph jobGraph = env.getStreamGraph().getJobGraph();

		JobSubmissionResult jobSubmissionResult = client.submitJob(jobGraph, SavepointMigrationTestBase.class.getClassLoader());

		LOG.info("Submitted job {} and waiting...", jobSubmissionResult.getJobID());

		boolean done = false;
		while (DEADLINE.hasTimeLeft()) {
			Thread.sleep(100);
			Map<String, OptionalFailure<Object>> accumulators = client.getAccumulators(jobSubmissionResult.getJobID());

			boolean allDone = true;
			for (Tuple2<String, Integer> acc : expectedAccumulators) {
				OptionalFailure<Object> accumOpt = accumulators.get(acc.f0);
				if (accumOpt == null) {
					allDone = false;
					break;
				}

				Integer numFinished = (Integer) accumOpt.get();
				if (numFinished == null) {
					allDone = false;
					break;
				}
				if (!numFinished.equals(acc.f1)) {
					allDone = false;
					break;
				}
			}
			if (allDone) {
				done = true;
				break;
			}
		}

		if (!done) {
			fail("Did not see the expected accumulator results within time limit.");
		}

		LOG.info("Triggering savepoint.");

		CompletableFuture<String> savepointPathFuture = client.triggerSavepoint(jobSubmissionResult.getJobID(), null);

		String jobmanagerSavepointPath = savepointPathFuture.get(DEADLINE.timeLeft().toMillis(), TimeUnit.MILLISECONDS);

		File jobManagerSavepoint = new File(new URI(jobmanagerSavepointPath).getPath());
		// savepoints were changed to be directories in Flink 1.3
		if (jobManagerSavepoint.isDirectory()) {
			FileUtils.moveDirectory(jobManagerSavepoint, new File(savepointPath));
		} else {
			FileUtils.moveFile(jobManagerSavepoint, new File(savepointPath));
		}
	}

	@SafeVarargs
	protected final void restoreAndExecute(
			StreamExecutionEnvironment env,
			String savepointPath,
			Tuple2<String, Integer>... expectedAccumulators) throws Exception {

		ClusterClient<?> client = miniClusterResource.getClusterClient();
		client.setDetached(true);

		// Submit the job
		JobGraph jobGraph = env.getStreamGraph().getJobGraph();

		jobGraph.setSavepointRestoreSettings(SavepointRestoreSettings.forPath(savepointPath));

		JobSubmissionResult jobSubmissionResult = client.submitJob(jobGraph, SavepointMigrationTestBase.class.getClassLoader());

		boolean done = false;
		while (DEADLINE.hasTimeLeft()) {

			// try and get a job result, this will fail if the job already failed. Use this
			// to get out of this loop
			JobID jobId = jobSubmissionResult.getJobID();

			try {
				CompletableFuture<JobStatus> jobStatusFuture = client.getJobStatus(jobSubmissionResult.getJobID());

				JobStatus jobStatus = jobStatusFuture.get(5, TimeUnit.SECONDS);

				assertNotEquals(JobStatus.FAILED, jobStatus);
			} catch (Exception e) {
				fail("Could not connect to job: " + e);
			}

			Thread.sleep(100);
			Map<String, OptionalFailure<Object>> accumulators = client.getAccumulators(jobId);

			boolean allDone = true;
			for (Tuple2<String, Integer> acc : expectedAccumulators) {
				OptionalFailure<Object> numFinished = accumulators.get(acc.f0);
				if (numFinished == null) {
					allDone = false;
					break;
				}
				if (!numFinished.get().equals(acc.f1)) {
					allDone = false;
					break;
				}
			}

			if (allDone) {
				done = true;
				break;
			}
		}

		if (!done) {
			fail("Did not see the expected accumulator results within time limit.");
		}
	}
}
