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

package org.apache.flink.table.descriptors;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.api.TableEnvironment;

/**
 * Describes a table connected from a batch environment.
 *
 * <p>This class just exists for backwards compatibility use {@link ConnectTableDescriptor} for
 * declarations.
 */
@PublicEvolving
public final class BatchTableDescriptor extends ConnectTableDescriptor {

	public BatchTableDescriptor(TableEnvironment tableEnv, ConnectorDescriptor connectorDescriptor) {
		super(tableEnv, connectorDescriptor);
	}

	@Override
	public BatchTableDescriptor withSchema(Schema schema) {
		return (BatchTableDescriptor) super.withSchema(schema);
	}
}
