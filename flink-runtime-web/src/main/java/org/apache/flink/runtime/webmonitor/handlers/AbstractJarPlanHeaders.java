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

package org.apache.flink.runtime.webmonitor.handlers;

import org.apache.flink.runtime.rest.messages.JobPlanInfo;
import org.apache.flink.runtime.rest.messages.MessageHeaders;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Message headers for {@link JarPlanHandler}.
 */
public abstract class AbstractJarPlanHeaders implements MessageHeaders<JarPlanRequestBody, JobPlanInfo, JarPlanMessageParameters> {

	@Override
	public Class<JobPlanInfo> getResponseClass() {
		return JobPlanInfo.class;
	}

	@Override
	public HttpResponseStatus getResponseStatusCode() {
		return HttpResponseStatus.OK;
	}

	@Override
	public Class<JarPlanRequestBody> getRequestClass() {
		return JarPlanRequestBody.class;
	}

	@Override
	public JarPlanMessageParameters getUnresolvedMessageParameters() {
		return new JarPlanMessageParameters();
	}

	@Override
	public String getTargetRestEndpointURL() {
		return "/jars/:" + JarIdPathParameter.KEY + "/plan";
	}

	@Override
	public String getDescription() {
		return "Returns the dataflow plan of a job contained in a jar previously uploaded via '" + JarUploadHeaders.URL + "'. " +
			"Program arguments can be passed both via the JSON request (recommended) or query parameters.";
	}
}
