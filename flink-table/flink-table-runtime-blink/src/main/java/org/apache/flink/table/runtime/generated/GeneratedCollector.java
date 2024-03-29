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

package org.apache.flink.table.runtime.generated;

import org.apache.flink.util.Collector;

/**
 * Describes a generated {@link Collector}.
 *
 * @param <C> type of collector
 */
public class GeneratedCollector<C extends Collector<?>> extends GeneratedClass<C> {

	private static final long serialVersionUID = -7355875544905245676L;

	/**
	 * Creates a GeneratedCollector.
	 *
	 * @param className class name of the generated Collector.
	 * @param code code of the generated Collector.
	 * @param references referenced objects of the generated Collector.
	 */
	public GeneratedCollector(String className, String code, Object[] references) {
		super(className, code, references);
	}
}
