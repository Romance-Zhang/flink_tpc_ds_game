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

package org.apache.flink.state.api.output;

import org.apache.flink.api.common.JobID;
import org.apache.flink.core.fs.Path;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.state.CheckpointStorageWorkerView;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.runtime.state.ttl.mock.MockStateBackend;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OperatorSnapshotFutures;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests that snapshot utils can properly snapshot an operator.
 */
public class SnapshotUtilsTest {

	private static final List<String> EXPECTED_CALL_OPERATOR_SNAPSHOT = Arrays.asList(
		"prepareSnapshotPreBarrier",
		"snapshotState",
		"notifyCheckpointComplete");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static final List<String> ACTUAL_ORDER_TRACKING =
		Collections.synchronizedList(new ArrayList<>(EXPECTED_CALL_OPERATOR_SNAPSHOT.size()));

	@Test
	public void testSnapshotUtilsLifecycle() throws Exception {
		StreamOperator<Void> operator 		= new LifecycleOperator();
		CheckpointStorageWorkerView storage = new MockStateBackend().createCheckpointStorage(new JobID());

		Path path = new Path(folder.newFolder().getAbsolutePath());

		SnapshotUtils.snapshot(operator, 0, 0L, storage, path);

		Assert.assertEquals(EXPECTED_CALL_OPERATOR_SNAPSHOT, ACTUAL_ORDER_TRACKING);
	}

	private static class LifecycleOperator implements StreamOperator<Void> {
		private static final long serialVersionUID = 1L;

		@Override
		public void open() throws Exception {
			ACTUAL_ORDER_TRACKING.add("open");
		}

		@Override
		public void close() throws Exception {
			ACTUAL_ORDER_TRACKING.add("close");
		}

		@Override
		public void dispose() throws Exception {
			ACTUAL_ORDER_TRACKING.add("dispose");
		}

		@Override
		public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
			ACTUAL_ORDER_TRACKING.add("prepareSnapshotPreBarrier");
		}

		@Override
		public OperatorSnapshotFutures snapshotState(long checkpointId, long timestamp, CheckpointOptions checkpointOptions, CheckpointStreamFactory storageLocation) throws Exception {
			ACTUAL_ORDER_TRACKING.add("snapshotState");
			return new OperatorSnapshotFutures();
		}

		@Override
		public void initializeState() throws Exception {
			ACTUAL_ORDER_TRACKING.add("initializeState");
		}

		@Override
		public void setKeyContextElement1(StreamRecord<?> record) throws Exception {
			ACTUAL_ORDER_TRACKING.add("setKeyContextElement1");
		}

		@Override
		public void setKeyContextElement2(StreamRecord<?> record) throws Exception {
			ACTUAL_ORDER_TRACKING.add("setKeyContextElement2");
		}

		@Override
		public ChainingStrategy getChainingStrategy() {
			ACTUAL_ORDER_TRACKING.add("getChainingStrategy");
			return null;
		}

		@Override
		public void setChainingStrategy(ChainingStrategy strategy) {
			ACTUAL_ORDER_TRACKING.add("setChainingStrategy");
		}

		@Override
		public MetricGroup getMetricGroup() {
			ACTUAL_ORDER_TRACKING.add("getMetricGroup");
			return null;
		}

		@Override
		public OperatorID getOperatorID() {
			ACTUAL_ORDER_TRACKING.add("getOperatorID");
			return null;
		}

		@Override
		public void notifyCheckpointComplete(long checkpointId) throws Exception {
			ACTUAL_ORDER_TRACKING.add("notifyCheckpointComplete");
		}

		@Override
		public void setCurrentKey(Object key) {
			ACTUAL_ORDER_TRACKING.add("setCurrentKey");
		}

		@Override
		public Object getCurrentKey() {
			ACTUAL_ORDER_TRACKING.add("getCurrentKey");
			return null;
		}
	}
}
