/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.kinesis.testutils;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.core.testutils.OneShotLatch;
import org.apache.flink.metrics.groups.UnregisteredMetricsGroup;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kinesis.internals.KinesisDataFetcher;
import org.apache.flink.streaming.connectors.kinesis.model.KinesisStreamShardState;
import org.apache.flink.streaming.connectors.kinesis.model.StreamShardHandle;
import org.apache.flink.streaming.connectors.kinesis.proxy.KinesisProxyInterface;
import org.apache.flink.streaming.connectors.kinesis.serialization.KinesisDeserializationSchema;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Extension of the {@link KinesisDataFetcher} for testing.
 */
public class TestableKinesisDataFetcher<T> extends KinesisDataFetcher<T> {

	private OneShotLatch runWaiter;
	private OneShotLatch initialDiscoveryWaiter;
	private OneShotLatch shutdownWaiter;

	private volatile boolean running;

	public TestableKinesisDataFetcher(
			List<String> fakeStreams,
			SourceFunction.SourceContext<T> sourceContext,
			Properties fakeConfiguration,
			KinesisDeserializationSchema<T> deserializationSchema,
			int fakeTotalCountOfSubtasks,
			int fakeIndexOfThisSubtask,
			AtomicReference<Throwable> thrownErrorUnderTest,
			LinkedList<KinesisStreamShardState> subscribedShardsStateUnderTest,
			HashMap<String, String> subscribedStreamsToLastDiscoveredShardIdsStateUnderTest,
			KinesisProxyInterface fakeKinesis) {
		super(
			fakeStreams,
			sourceContext,
			sourceContext.getCheckpointLock(),
			getMockedRuntimeContext(fakeTotalCountOfSubtasks, fakeIndexOfThisSubtask),
			fakeConfiguration,
			deserializationSchema,
			DEFAULT_SHARD_ASSIGNER,
			null,
			null,
			thrownErrorUnderTest,
			subscribedShardsStateUnderTest,
			subscribedStreamsToLastDiscoveredShardIdsStateUnderTest,
			(properties) -> fakeKinesis);

		this.runWaiter = new OneShotLatch();
		this.initialDiscoveryWaiter = new OneShotLatch();
		this.shutdownWaiter = new OneShotLatch();

		this.running = true;
	}

	@Override
	public void runFetcher() throws Exception {
		runWaiter.trigger();
		super.runFetcher();
	}

	public void waitUntilRun() throws Exception {
		runWaiter.await();
	}

	public void waitUntilShutdown(long timeout, TimeUnit timeUnit) throws Exception {
		shutdownWaiter.await(timeout, timeUnit);
	}

	@Override
	protected ExecutorService createShardConsumersThreadPool(String subtaskName) {
		// this is just a dummy fetcher, so no need to create a thread pool for shard consumers
		ExecutorService mockExecutor = mock(ExecutorService.class);
		when(mockExecutor.isTerminated()).thenAnswer((InvocationOnMock invocation) -> !running);
		try {
			when(mockExecutor.awaitTermination(anyLong(), any())).thenReturn(!running);
		} catch (InterruptedException e) {
			// We're just trying to stub the method. Must acknowledge the checked exception.
		}
		return mockExecutor;
	}

	@Override
	public void awaitTermination() throws InterruptedException {
		this.running = false;
		super.awaitTermination();
	}

	@Override
	public void shutdownFetcher() {
		super.shutdownFetcher();
		shutdownWaiter.trigger();
	}

	@Override
	public List<StreamShardHandle> discoverNewShardsToSubscribe() throws InterruptedException {
		List<StreamShardHandle> newShards = super.discoverNewShardsToSubscribe();
		initialDiscoveryWaiter.trigger();
		return newShards;
	}

	public void waitUntilInitialDiscovery() throws InterruptedException {
		initialDiscoveryWaiter.await();
	}

	private static RuntimeContext getMockedRuntimeContext(final int fakeTotalCountOfSubtasks, final int fakeIndexOfThisSubtask) {
		RuntimeContext mockedRuntimeContext = mock(RuntimeContext.class);

		Mockito.when(mockedRuntimeContext.getNumberOfParallelSubtasks()).thenReturn(fakeTotalCountOfSubtasks);
		Mockito.when(mockedRuntimeContext.getIndexOfThisSubtask()).thenReturn(fakeIndexOfThisSubtask);
		Mockito.when(mockedRuntimeContext.getTaskName()).thenReturn("Fake Task");
		Mockito.when(mockedRuntimeContext.getTaskNameWithSubtasks()).thenReturn(
				"Fake Task (" + fakeIndexOfThisSubtask + "/" + fakeTotalCountOfSubtasks + ")");
		Mockito.when(mockedRuntimeContext.getUserCodeClassLoader()).thenReturn(
				Thread.currentThread().getContextClassLoader());

		Mockito.when(mockedRuntimeContext.getMetricGroup()).thenReturn(new UnregisteredMetricsGroup());

		return mockedRuntimeContext;
	}
}
