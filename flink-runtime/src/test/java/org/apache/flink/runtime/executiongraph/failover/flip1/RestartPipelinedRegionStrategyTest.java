/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.executiongraph.failover.flip1;

import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.io.network.partition.PartitionNotFoundException;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.io.network.partition.consumer.PartitionConnectionException;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.scheduler.strategy.ExecutionVertexID;
import org.apache.flink.util.TestLogger;

import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

/**
 * Tests the failure handling logic of the {@link RestartPipelinedRegionStrategy}.
 */
public class RestartPipelinedRegionStrategyTest extends TestLogger {

	/**
	 * Tests for scenes that a task fails for its own error, in which case the
	 * region containing the failed task and its consumer regions should be restarted.
	 * <pre>
	 *     (v1) -+-> (v4)
	 *           x
	 *     (v2) -+-> (v5)
	 *
	 *     (v3) -+-> (v6)
	 *
	 *           ^
	 *           |
	 *       (blocking)
	 * </pre>
	 * Each vertex is in an individual region.
	 */
	@Test
	public void testRegionFailoverForRegionInternalErrors() throws Exception {
		TestFailoverTopology.Builder topologyBuilder = new TestFailoverTopology.Builder();

		TestFailoverTopology.TestFailoverVertex v1 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v2 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v3 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v4 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v5 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v6 = topologyBuilder.newVertex();

		topologyBuilder.connect(v1, v4, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v1, v5, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v2, v4, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v2, v5, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v3, v6, ResultPartitionType.BLOCKING);

		FailoverTopology topology = topologyBuilder.build();

		RestartPipelinedRegionStrategy strategy = new RestartPipelinedRegionStrategy(topology);

		// when v1 fails, {v1,v4,v5} should be restarted
		HashSet<ExecutionVertexID> expectedResult = new HashSet<>();
		expectedResult.add(v1.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v1.getExecutionVertexID(), new Exception("Test failure")));

		// when v2 fails, {v2,v4,v5} should be restarted
		expectedResult.clear();
		expectedResult.add(v2.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v2.getExecutionVertexID(), new Exception("Test failure")));

		// when v3 fails, {v3,v6} should be restarted
		expectedResult.clear();
		expectedResult.add(v3.getExecutionVertexID());
		expectedResult.add(v6.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")));

		// when v4 fails, {v4} should be restarted
		expectedResult.clear();
		expectedResult.add(v4.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v4.getExecutionVertexID(), new Exception("Test failure")));

		// when v5 fails, {v5} should be restarted
		expectedResult.clear();
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v5.getExecutionVertexID(), new Exception("Test failure")));

		// when v6 fails, {v6} should be restarted
		expectedResult.clear();
		expectedResult.add(v6.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v6.getExecutionVertexID(), new Exception("Test failure")));
	}

	/**
	 * Tests for scenes that a task fails for data consumption error, in which case the
	 * region containing the failed task, the region containing the unavailable result partition
	 * and all their consumer regions should be restarted.
	 * <pre>
	 *     (v1) -+-> (v4)
	 *           x
	 *     (v2) -+-> (v5)
	 *
	 *     (v3) -+-> (v6)
	 *
	 *           ^
	 *           |
	 *       (blocking)
	 * </pre>
	 * Each vertex is in an individual region.
	 */
	@Test
	public void testRegionFailoverForDataConsumptionErrors() throws Exception {
		TestFailoverTopology.Builder topologyBuilder = new TestFailoverTopology.Builder();

		TestFailoverTopology.TestFailoverVertex v1 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v2 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v3 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v4 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v5 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v6 = topologyBuilder.newVertex();

		topologyBuilder.connect(v1, v4, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v1, v5, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v2, v4, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v2, v5, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v3, v6, ResultPartitionType.BLOCKING);

		FailoverTopology topology = topologyBuilder.build();

		RestartPipelinedRegionStrategy strategy = new RestartPipelinedRegionStrategy(topology);

		// when v4 fails to consume data from v1, {v1,v4,v5} should be restarted
		HashSet<ExecutionVertexID> expectedResult = new HashSet<>();
		Iterator<? extends FailoverEdge> v4InputEdgeIterator = v4.getInputEdges().iterator();
		expectedResult.add(v1.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v4.getExecutionVertexID(),
				new PartitionConnectionException(
					new ResultPartitionID(
						v4InputEdgeIterator.next().getResultPartitionID(),
						new ExecutionAttemptID()),
					new Exception("Test failure"))));

		// when v4 fails to consume data from v2, {v2,v4,v5} should be restarted
		expectedResult.clear();
		expectedResult.add(v2.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v4.getExecutionVertexID(),
				new PartitionNotFoundException(
					new ResultPartitionID(
						v4InputEdgeIterator.next().getResultPartitionID(),
						new ExecutionAttemptID()))));

		// when v5 fails to consume data from v1, {v1,v4,v5} should be restarted
		expectedResult.clear();
		Iterator<? extends FailoverEdge> v5InputEdgeIterator = v5.getInputEdges().iterator();
		expectedResult.add(v1.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v5.getExecutionVertexID(),
				new PartitionConnectionException(
					new ResultPartitionID(
						v5InputEdgeIterator.next().getResultPartitionID(),
						new ExecutionAttemptID()),
					new Exception("Test failure"))));

		// when v5 fails to consume data from v2, {v2,v4,v5} should be restarted
		expectedResult.clear();
		expectedResult.add(v2.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v5.getExecutionVertexID(),
				new PartitionNotFoundException(
					new ResultPartitionID(
						v5InputEdgeIterator.next().getResultPartitionID(),
						new ExecutionAttemptID()))));

		// when v6 fails to consume data from v3, {v3,v6} should be restarted
		expectedResult.clear();
		Iterator<? extends FailoverEdge> v6InputEdgeIterator = v6.getInputEdges().iterator();
		expectedResult.add(v3.getExecutionVertexID());
		expectedResult.add(v6.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v6.getExecutionVertexID(),
				new PartitionConnectionException(
					new ResultPartitionID(
						v6InputEdgeIterator.next().getResultPartitionID(),
						new ExecutionAttemptID()),
					new Exception("Test failure"))));
	}

	/**
	 * Tests to verify region failover results regarding different input result partition availability combinations.
	 * <pre>
	 *     (v1) --rp1--\
	 *                 (v3)
	 *     (v2) --rp2--/
	 *
	 *             ^
	 *             |
	 *         (blocking)
	 * </pre>
	 * Each vertex is in an individual region.
	 * rp1, rp2 are result partitions.
	 */
	@Test
	public void testRegionFailoverForVariousResultPartitionAvailabilityCombinations() throws Exception {
		TestFailoverTopology.Builder topologyBuilder = new TestFailoverTopology.Builder();

		TestFailoverTopology.TestFailoverVertex v1 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v2 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v3 = topologyBuilder.newVertex();

		topologyBuilder.connect(v1, v3, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v2, v3, ResultPartitionType.BLOCKING);

		FailoverTopology topology = topologyBuilder.build();

		TestResultPartitionAvailabilityChecker availabilityChecker = new TestResultPartitionAvailabilityChecker();
		RestartPipelinedRegionStrategy strategy = new RestartPipelinedRegionStrategy(topology, availabilityChecker);

		IntermediateResultPartitionID rp1ID = v1.getOutputEdges().iterator().next().getResultPartitionID();
		IntermediateResultPartitionID rp2ID = v2.getOutputEdges().iterator().next().getResultPartitionID();

		// -------------------------------------------------
		// Combination1: (rp1 == available, rp == available)
		// -------------------------------------------------
		availabilityChecker.failedPartitions.clear();

		// when v1 fails, {v1,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v1.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v2 fails, {v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v2.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v3 fails, {v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v3.getExecutionVertexID()));

		// -------------------------------------------------
		// Combination2: (rp1 == unavailable, rp == available)
		// -------------------------------------------------
		availabilityChecker.failedPartitions.clear();
		availabilityChecker.markResultPartitionFailed(rp1ID);

		// when v1 fails, {v1,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v1.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v2 fails, {v1,v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v2.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v3 fails, {v1,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v3.getExecutionVertexID()));

		// -------------------------------------------------
		// Combination3: (rp1 == available, rp == unavailable)
		// -------------------------------------------------
		availabilityChecker.failedPartitions.clear();
		availabilityChecker.markResultPartitionFailed(rp2ID);

		// when v1 fails, {v1,v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v1.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v2 fails, {v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v2.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v3 fails, {v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// -------------------------------------------------
		// Combination4: (rp1 == unavailable, rp == unavailable)
		// -------------------------------------------------
		availabilityChecker.failedPartitions.clear();
		availabilityChecker.markResultPartitionFailed(rp1ID);
		availabilityChecker.markResultPartitionFailed(rp2ID);

		// when v1 fails, {v1,v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v1.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v2 fails, {v1,v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v2.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v2.getExecutionVertexID(), v3.getExecutionVertexID()));

		// when v3 fails, {v1,v2,v3} should be restarted
		assertThat(
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")),
			containsInAnyOrder(v1.getExecutionVertexID(), v2.getExecutionVertexID(), v3.getExecutionVertexID()));
	}

	/**
	 * Tests region failover scenes for topology with multiple vertices.
	 * <pre>
	 *     (v1) ---> (v2) --|--> (v3) ---> (v4) --|--> (v5) ---> (v6)
	 *
	 *           ^          ^          ^          ^          ^
	 *           |          |          |          |          |
	 *     (pipelined) (blocking) (pipelined) (blocking) (pipelined)
	 * </pre>
	 * Component 1: 1,2; component 2: 3,4; component 3: 5,6
	 */
	@Test
	public void testRegionFailoverForMultipleVerticesRegions() throws Exception {
		TestFailoverTopology.Builder topologyBuilder = new TestFailoverTopology.Builder();

		TestFailoverTopology.TestFailoverVertex v1 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v2 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v3 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v4 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v5 = topologyBuilder.newVertex();
		TestFailoverTopology.TestFailoverVertex v6 = topologyBuilder.newVertex();

		topologyBuilder.connect(v1, v2, ResultPartitionType.PIPELINED);
		topologyBuilder.connect(v2, v3, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v3, v4, ResultPartitionType.PIPELINED);
		topologyBuilder.connect(v4, v5, ResultPartitionType.BLOCKING);
		topologyBuilder.connect(v5, v6, ResultPartitionType.PIPELINED);

		FailoverTopology topology = topologyBuilder.build();

		RestartPipelinedRegionStrategy strategy = new RestartPipelinedRegionStrategy(topology);

		// when v3 fails due to internal error, {v3,v4,v5,v6} should be restarted
		HashSet<ExecutionVertexID> expectedResult = new HashSet<>();
		expectedResult.add(v3.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		expectedResult.add(v6.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(), new Exception("Test failure")));

		// when v3 fails to consume from v2, {v1,v2,v3,v4,v5,v6} should be restarted
		expectedResult.clear();
		expectedResult.add(v1.getExecutionVertexID());
		expectedResult.add(v2.getExecutionVertexID());
		expectedResult.add(v3.getExecutionVertexID());
		expectedResult.add(v4.getExecutionVertexID());
		expectedResult.add(v5.getExecutionVertexID());
		expectedResult.add(v6.getExecutionVertexID());
		assertEquals(expectedResult,
			strategy.getTasksNeedingRestart(v3.getExecutionVertexID(),
				new PartitionConnectionException(
					new ResultPartitionID(
						v3.getInputEdges().iterator().next().getResultPartitionID(),
						new ExecutionAttemptID()),
					new Exception("Test failure"))));
	}

	// ------------------------------------------------------------------------
	//  utilities
	// ------------------------------------------------------------------------

	private static class TestResultPartitionAvailabilityChecker implements ResultPartitionAvailabilityChecker {

		private final HashSet<IntermediateResultPartitionID> failedPartitions;

		public TestResultPartitionAvailabilityChecker() {
			this.failedPartitions = new HashSet<>();
		}

		@Override
		public boolean isAvailable(IntermediateResultPartitionID resultPartitionID) {
			return !failedPartitions.contains(resultPartitionID);
		}

		public void markResultPartitionFailed(IntermediateResultPartitionID resultPartitionID) {
			failedPartitions.add(resultPartitionID);
		}

		public void removeResultPartitionFromFailedState(IntermediateResultPartitionID resultPartitionID) {
			failedPartitions.remove(resultPartitionID);
		}
	}
}
