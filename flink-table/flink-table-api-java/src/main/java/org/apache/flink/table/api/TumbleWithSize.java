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

package org.apache.flink.table.api;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.ExpressionParser;

/**
 * Tumbling window.
 *
 * <p>For streaming tables you can specify grouping by a event-time or processing-time attribute.
 *
 * <p>For batch tables you can specify grouping on a timestamp or long attribute.
 */
@PublicEvolving
public final class TumbleWithSize {

	/** The size of the window either as time or row-count interval. */
	private Expression size;

	TumbleWithSize(Expression size) {
		this.size = size;
	}

	/**
	 * Specifies the time attribute on which rows are grouped.
	 *
	 * <p>For streaming tables you can specify grouping by a event-time or processing-time
	 * attribute.
	 *
	 * <p>For batch tables you can specify grouping on a timestamp or long attribute.
	 *
	 * @param timeField time attribute for streaming and batch tables
	 * @return a tumbling window on event-time
	 */
	public TumbleWithSizeOnTime on(Expression timeField) {
		return new TumbleWithSizeOnTime(timeField, size);
	}

	/**
	 * Specifies the time attribute on which rows are grouped.
	 *
	 * <p>For streaming tables you can specify grouping by a event-time or processing-time
	 * attribute.
	 *
	 * <p>For batch tables you can specify grouping on a timestamp or long attribute.
	 *
	 * @param timeField time attribute for streaming and batch tables
	 * @return a tumbling window on event-time
	 */
	public TumbleWithSizeOnTime on(String timeField) {
		return on(ExpressionParser.parseExpression(timeField));
	}
}
