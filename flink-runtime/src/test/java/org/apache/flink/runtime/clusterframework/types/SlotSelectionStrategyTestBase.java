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

package org.apache.flink.runtime.clusterframework.types;

import org.apache.flink.runtime.executiongraph.utils.SimpleAckingTaskManagerGateway;
import org.apache.flink.runtime.instance.SimpleSlotContext;
import org.apache.flink.runtime.jobmanager.slots.TaskManagerGateway;
import org.apache.flink.runtime.jobmaster.slotpool.SlotSelectionStrategy;
import org.apache.flink.runtime.taskmanager.TaskManagerLocation;
import org.apache.flink.util.TestLogger;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class SlotSelectionStrategyTestBase extends TestLogger {

	protected final ResourceProfile resourceProfile = new ResourceProfile(2, 1024);
	protected final ResourceProfile biggerResourceProfile = new ResourceProfile(3, 1024);

	protected final AllocationID aid1 = new AllocationID();
	protected final AllocationID aid2 = new AllocationID();
	protected final AllocationID aid3 = new AllocationID();
	protected final AllocationID aid4 = new AllocationID();
	protected final AllocationID aidX = new AllocationID();

	protected final TaskManagerLocation tml1 = new TaskManagerLocation(new ResourceID("tm-1"), InetAddress.getLoopbackAddress(), 42);
	protected final TaskManagerLocation tml2 = new TaskManagerLocation(new ResourceID("tm-2"), InetAddress.getLoopbackAddress(), 43);
	protected final TaskManagerLocation tml3 = new TaskManagerLocation(new ResourceID("tm-3"), InetAddress.getLoopbackAddress(), 44);
	protected final TaskManagerLocation tml4 = new TaskManagerLocation(new ResourceID("tm-4"), InetAddress.getLoopbackAddress(), 45);
	protected final TaskManagerLocation tmlX = new TaskManagerLocation(new ResourceID("tm-X"), InetAddress.getLoopbackAddress(), 46);

	protected final TaskManagerGateway taskManagerGateway = new SimpleAckingTaskManagerGateway();

	protected final SimpleSlotContext ssc1 = new SimpleSlotContext(aid1, tml1, 1, taskManagerGateway, resourceProfile);
	protected final SimpleSlotContext ssc2 = new SimpleSlotContext(aid2, tml2, 2, taskManagerGateway, biggerResourceProfile);
	protected final SimpleSlotContext ssc3 = new SimpleSlotContext(aid3, tml3, 3, taskManagerGateway, resourceProfile);
	protected final SimpleSlotContext ssc4 = new SimpleSlotContext(aid4, tml4, 4, taskManagerGateway, resourceProfile);

	protected final Set<SlotSelectionStrategy.SlotInfoAndResources> candidates = Collections.unmodifiableSet(createCandidates());

	protected final SlotSelectionStrategy selectionStrategy;

	public SlotSelectionStrategyTestBase(SlotSelectionStrategy slotSelectionStrategy) {
		this.selectionStrategy = slotSelectionStrategy;
	}

	private Set<SlotSelectionStrategy.SlotInfoAndResources> createCandidates() {
		Set<SlotSelectionStrategy.SlotInfoAndResources> candidates = new HashSet<>(4);
		candidates.add(new SlotSelectionStrategy.SlotInfoAndResources(ssc1));
		candidates.add(new SlotSelectionStrategy.SlotInfoAndResources(ssc2));
		candidates.add(new SlotSelectionStrategy.SlotInfoAndResources(ssc3));
		candidates.add(new SlotSelectionStrategy.SlotInfoAndResources(ssc4));
		return candidates;
	}

	protected Optional<SlotSelectionStrategy.SlotInfoAndLocality> runMatching(SlotProfile slotProfile) {
		return selectionStrategy.selectBestSlotForProfile(candidates, slotProfile);
	}
}
