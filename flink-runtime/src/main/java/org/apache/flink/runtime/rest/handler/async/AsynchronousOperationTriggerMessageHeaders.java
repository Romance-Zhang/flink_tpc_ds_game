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

package org.apache.flink.runtime.rest.handler.async;

import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.rest.messages.MessageParameters;
import org.apache.flink.runtime.rest.messages.RequestBody;

/**
 * Message headers for the triggering of an asynchronous operation.
 *
 * @param <R> type of the request
 * @param <M> type of the message parameters
 */
public abstract class AsynchronousOperationTriggerMessageHeaders<R extends RequestBody, M extends MessageParameters>
	implements MessageHeaders<R, TriggerResponse, M> {

	@Override
	public Class<TriggerResponse> getResponseClass() {
		return TriggerResponse.class;
	}

	@Override
	public String getDescription() {
		return getAsyncOperationDescription() + " This async operation would return a 'triggerid' for further query identifier.";
	}

	/**
	 * Returns the description for this async operation header.
	 *
	 * @return the description for this async operation header.
	 */
	protected abstract String getAsyncOperationDescription();
}
