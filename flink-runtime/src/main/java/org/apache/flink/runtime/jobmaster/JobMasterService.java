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

import org.apache.flink.runtime.messages.Acknowledge;
import org.apache.flink.util.AutoCloseableAsync;

import java.util.concurrent.CompletableFuture;

/**
 * Interface which specifies the JobMaster service.
 */
public interface JobMasterService extends AutoCloseableAsync {

	/**
	 * Start the JobMaster service with the given {@link JobMasterId}.
	 *
	 * @param jobMasterId to start the service with
	 * @return Future which is completed once the JobMaster service has been started
	 * @throws Exception if the JobMaster service could not be started
	 */
	CompletableFuture<Acknowledge> start(JobMasterId jobMasterId) throws Exception;

	/**
	 * Suspend the JobMaster service. This means that the service will stop to react
	 * to messages.
	 *
	 * @param cause for the suspension
	 * @return Future which is completed once the JobMaster service has been suspended
	 */
	CompletableFuture<Acknowledge> suspend(Exception cause);

	/**
	 * Get the {@link JobMasterGateway} belonging to this service.
	 *
	 * @return JobMasterGateway belonging to this service
	 */
	JobMasterGateway getGateway();

	/**
	 * Get the address of the JobMaster service under which it is reachable.
	 *
	 * @return Address of the JobMaster service
	 */
	String getAddress();
}
