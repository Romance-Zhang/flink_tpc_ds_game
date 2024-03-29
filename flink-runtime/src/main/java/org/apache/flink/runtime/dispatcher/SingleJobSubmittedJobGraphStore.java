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

package org.apache.flink.runtime.dispatcher;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobmanager.SubmittedJobGraph;
import org.apache.flink.runtime.jobmanager.SubmittedJobGraphStore;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.Preconditions;

import java.util.Collection;
import java.util.Collections;

/**
 * {@link SubmittedJobGraphStore} implementation for a single job.
 */
public class SingleJobSubmittedJobGraphStore implements SubmittedJobGraphStore {

	private final JobGraph jobGraph;

	public SingleJobSubmittedJobGraphStore(JobGraph jobGraph) {
		this.jobGraph = Preconditions.checkNotNull(jobGraph);
	}

	@Override
	public void start(SubmittedJobGraphListener jobGraphListener) throws Exception {
		// noop
	}

	@Override
	public void stop() throws Exception {
		// noop
	}

	@Override
	public SubmittedJobGraph recoverJobGraph(JobID jobId) throws Exception {
		if (jobGraph.getJobID().equals(jobId)) {
			return new SubmittedJobGraph(jobGraph);
		} else {
			throw new FlinkException("Could not recover job graph " + jobId + '.');
		}
	}

	@Override
	public void putJobGraph(SubmittedJobGraph jobGraph) throws Exception {
		if (!jobGraph.getJobId().equals(jobGraph.getJobId())) {
			throw new FlinkException("Cannot put additional jobs into this submitted job graph store.");
		}
	}

	@Override
	public void removeJobGraph(JobID jobId) {
		// ignore
	}

	@Override
	public void releaseJobGraph(JobID jobId) {
		// ignore
	}

	@Override
	public Collection<JobID> getJobIds() {
		return Collections.singleton(jobGraph.getJobID());
	}
}
