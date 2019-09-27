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

package org.apache.flink.runtime.jobmanager;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.jobgraph.JobGraph;

import java.util.Collection;
import java.util.Collections;

/**
 * {@link SubmittedJobGraph} instances for JobManagers running in {@link HighAvailabilityMode#NONE}.
 *
 * <p>All operations are NoOps, because {@link JobGraph} instances cannot be recovered in this
 * recovery mode.
 */
public class StandaloneSubmittedJobGraphStore implements SubmittedJobGraphStore {

	@Override
	public void start(SubmittedJobGraphListener jobGraphListener) throws Exception {
		// Nothing to do
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	@Override
	public void putJobGraph(SubmittedJobGraph jobGraph) {
		// Nothing to do
	}

	@Override
	public void removeJobGraph(JobID jobId) {
		// Nothing to do
	}

	@Override
	public void releaseJobGraph(JobID jobId) {
		// nothing to do
	}

	@Override
	public Collection<JobID> getJobIds() {
		return Collections.emptyList();
	}

	@Override
	public SubmittedJobGraph recoverJobGraph(JobID jobId) {
		return null;
	}
}
