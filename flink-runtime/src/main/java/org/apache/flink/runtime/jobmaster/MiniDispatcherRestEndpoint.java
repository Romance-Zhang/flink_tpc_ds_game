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

package org.apache.flink.runtime.jobmaster;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.blob.TransientBlobService;
import org.apache.flink.runtime.entrypoint.JobClusterEntrypoint;
import org.apache.flink.runtime.leaderelection.LeaderElectionService;
import org.apache.flink.runtime.resourcemanager.ResourceManagerGateway;
import org.apache.flink.runtime.rest.RestServerEndpointConfiguration;
import org.apache.flink.runtime.rest.handler.RestHandlerConfiguration;
import org.apache.flink.runtime.rest.handler.legacy.metrics.MetricFetcher;
import org.apache.flink.runtime.rpc.FatalErrorHandler;
import org.apache.flink.runtime.webmonitor.RestfulGateway;
import org.apache.flink.runtime.webmonitor.WebMonitorEndpoint;
import org.apache.flink.runtime.webmonitor.retriever.GatewayRetriever;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * REST endpoint for the {@link JobClusterEntrypoint}.
 */
public class MiniDispatcherRestEndpoint extends WebMonitorEndpoint<RestfulGateway> {

	public MiniDispatcherRestEndpoint(
			RestServerEndpointConfiguration endpointConfiguration,
			GatewayRetriever<? extends RestfulGateway> leaderRetriever,
			Configuration clusterConfiguration,
			RestHandlerConfiguration restConfiguration,
			GatewayRetriever<ResourceManagerGateway> resourceManagerRetriever,
			TransientBlobService transientBlobService,
			ExecutorService executor,
			MetricFetcher metricFetcher,
			LeaderElectionService leaderElectionService,
			FatalErrorHandler fatalErrorHandler) throws IOException {
		super(
			endpointConfiguration,
			leaderRetriever,
			clusterConfiguration,
			restConfiguration,
			resourceManagerRetriever,
			transientBlobService,
			executor,
			metricFetcher,
			leaderElectionService,
			fatalErrorHandler);
	}
}
