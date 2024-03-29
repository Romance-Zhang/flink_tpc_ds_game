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

package org.apache.flink.runtime.resourcemanager;

import org.apache.flink.runtime.clusterframework.ApplicationStatus;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.entrypoint.ClusterInformation;
import org.apache.flink.runtime.heartbeat.HeartbeatServices;
import org.apache.flink.runtime.highavailability.HighAvailabilityServices;
import org.apache.flink.runtime.metrics.MetricRegistry;
import org.apache.flink.runtime.metrics.groups.JobManagerMetricGroup;
import org.apache.flink.runtime.resourcemanager.exceptions.ResourceManagerException;
import org.apache.flink.runtime.resourcemanager.slotmanager.SlotManager;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.rpc.RpcService;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple {@link ResourceManager} implementation for testing purposes.
 */
public class TestingResourceManager extends ResourceManager<ResourceID> {

	public TestingResourceManager(
			RpcService rpcService,
			String resourceManagerEndpointId,
			ResourceID resourceId,
			HighAvailabilityServices highAvailabilityServices,
			HeartbeatServices heartbeatServices,
			SlotManager slotManager,
			MetricRegistry metricRegistry,
			JobLeaderIdService jobLeaderIdService,
			FatalErrorHandler fatalErrorHandler,
			JobManagerMetricGroup jobManagerMetricGroup) {
		super(
			rpcService,
			resourceManagerEndpointId,
			resourceId,
			highAvailabilityServices,
			heartbeatServices,
			slotManager,
			metricRegistry,
			jobLeaderIdService,
			new ClusterInformation("localhost", 1234),
			fatalErrorHandler,
			jobManagerMetricGroup);
	}

	@Override
	protected void initialize() throws ResourceManagerException {
		// noop
	}

	@Override
	protected void internalDeregisterApplication(ApplicationStatus finalStatus, @Nullable String diagnostics) throws ResourceManagerException {
		// noop
	}

	@Override
	public Collection<ResourceProfile> startNewWorker(ResourceProfile resourceProfile) {
		return Collections.emptyList();
	}

	@Override
	protected ResourceID workerStarted(ResourceID resourceID) {
		return resourceID;
	}

	@Override
	public boolean stopWorker(ResourceID worker) {
		// cannot stop workers
		return false;
	}
}
