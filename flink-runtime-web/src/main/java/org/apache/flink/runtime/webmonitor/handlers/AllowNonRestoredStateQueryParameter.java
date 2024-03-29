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

import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.rest.messages.MessageQueryParameter;

/**
 * Query parameter that specifies whether non restored state is allowed if the savepoint
 * contains state for an operator that is not part of the job.
 *
 * @see SavepointRestoreSettings#allowNonRestoredState()
 */
public class AllowNonRestoredStateQueryParameter extends MessageQueryParameter<Boolean> {

	protected AllowNonRestoredStateQueryParameter() {
		super("allowNonRestoredState", MessageParameterRequisiteness.OPTIONAL);
	}

	@Override
	public Boolean convertStringToValue(final String value) {
		return Boolean.valueOf(value);
	}

	@Override
	public String convertValueToString(final Boolean value) {
		return value.toString();
	}

	@Override
	public String getDescription() {
		return "Boolean value that specifies whether the job submission should be rejected " +
			"if the savepoint contains state that cannot be mapped back to the job.";
	}
}
