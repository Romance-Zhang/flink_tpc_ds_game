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

package org.apache.flink.runtime.io.network;

import java.io.IOException;

/**
 * A dummy implementation of the {@link ConnectionManager} which is mainly used for creating
 * {@link PartitionRequestClient} instance in tests.
 */
public class TestingConnectionManager implements ConnectionManager {

	@Override
	public int start() {
		return -1;
	}

	@Override
	public PartitionRequestClient createPartitionRequestClient(ConnectionID connectionId) throws IOException {
		return new TestingPartitionRequestClient();
	}

	@Override
	public void closeOpenChannelConnections(ConnectionID connectionId) {}

	@Override
	public int getNumberOfActiveConnections() {
		return 0;
	}

	@Override
	public void shutdown() {}
}
