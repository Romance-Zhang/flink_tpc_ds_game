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

package org.apache.flink.runtime.rest.messages;

/**
 * {@link MessagePathParameter} for the trigger id of an asynchronous operation.
 */
public class TriggerIdPathParameter extends MessagePathParameter<TriggerId> {

	public static final String KEY = "triggerid";

	public TriggerIdPathParameter() {
		super(KEY);
	}

	@Override
	protected TriggerId convertFromString(String value) throws ConversionException {
		return TriggerId.fromHexString(value);
	}

	@Override
	protected String convertToString(TriggerId value) {
		return value.toString();
	}

	@Override
	public String getDescription() {
		return "32-character hexadecimal string that identifies an asynchronous operation trigger ID. " +
			"The ID was returned then the operation was triggered.";
	}
}
