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

package org.apache.flink.queryablestate.network;

import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.queryablestate.network.messages.MessageBody;
import org.apache.flink.queryablestate.network.messages.MessageDeserializer;
import org.apache.flink.queryablestate.network.messages.MessageSerializer;
import org.apache.flink.queryablestate.network.stats.AtomicKvStateRequestStats;
import org.apache.flink.queryablestate.network.stats.DisabledKvStateRequestStats;
import org.apache.flink.queryablestate.network.stats.KvStateRequestStats;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.TestLogger;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tests general behavior of the {@link AbstractServerBase}.
 */
public class AbstractServerTest extends TestLogger {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	/**
	 * Tests that in case of port collision, a FlinkRuntimeException is thrown
	 * with a specific message.
	 */
	@Test
	public void testServerInitializationFailure() throws Throwable {

		// the expected exception along with the adequate message
		expectedEx.expect(FlinkRuntimeException.class);
		expectedEx.expectMessage("Unable to start Test Server 2. All ports in provided range are occupied.");

		List<Integer> portList = new ArrayList<>();
		portList.add(7777);

		try (TestServer server1 = new TestServer("Test Server 1", new DisabledKvStateRequestStats(), portList.iterator())) {
			server1.start();

			try (TestServer server2 = new TestServer("Test Server 2", new DisabledKvStateRequestStats(),
					Collections.singletonList(server1.getServerAddress().getPort()).iterator())) {
				server2.start();
			}
		}
	}

	/**
	 * Tests that in case of port collision and big enough port range,
	 * the server will try to bind to the next port in the range.
	 */
	@Test
	public void testPortRangeSuccess() throws Throwable {

		// this is shared between the two servers.
		AtomicKvStateRequestStats serverStats = new AtomicKvStateRequestStats();
		AtomicKvStateRequestStats clientStats = new AtomicKvStateRequestStats();

		List<Integer> portList = new ArrayList<>();
		portList.add(7777);
		portList.add(7778);
		portList.add(7779);

		try (
				TestServer server1 = new TestServer("Test Server 1", serverStats, portList.iterator());
				TestServer server2 = new TestServer("Test Server 2", serverStats, portList.iterator());
				TestClient client = new TestClient(
						"Test Client",
						1,
						new MessageSerializer<>(new TestMessage.TestMessageDeserializer(), new TestMessage.TestMessageDeserializer()),
						clientStats
				)
		) {
			server1.start();
			Assert.assertTrue(server1.getServerAddress().getPort() >= 7777 && server1.getServerAddress().getPort() <= 7779);

			server2.start();
			Assert.assertTrue(server2.getServerAddress().getPort() >= 7777 && server2.getServerAddress().getPort() <= 7779);

			TestMessage response1 = client.sendRequest(server1.getServerAddress(), new TestMessage("ping")).join();
			Assert.assertEquals(server1.getServerName() + "-ping", response1.getMessage());

			TestMessage response2 = client.sendRequest(server2.getServerAddress(), new TestMessage("pong")).join();
			Assert.assertEquals(server2.getServerName() + "-pong", response2.getMessage());

			// the client connects to both servers and the stats object is shared.
			Assert.assertEquals(2L, serverStats.getNumConnections());

			Assert.assertEquals(2L, clientStats.getNumConnections());
			Assert.assertEquals(0L, clientStats.getNumFailed());
			Assert.assertEquals(2L, clientStats.getNumSuccessful());
			Assert.assertEquals(2L, clientStats.getNumRequests());
		}

		Assert.assertEquals(0L, clientStats.getNumConnections());
		Assert.assertEquals(0L, clientStats.getNumFailed());
		Assert.assertEquals(2L, clientStats.getNumSuccessful());
		Assert.assertEquals(2L, clientStats.getNumRequests());
	}

	private static class TestClient extends Client<TestMessage, TestMessage> implements AutoCloseable {

		TestClient(
				String clientName,
				int numEventLoopThreads,
				MessageSerializer<TestMessage, TestMessage> serializer,
				KvStateRequestStats stats) {
			super(clientName, numEventLoopThreads, serializer, stats);
		}

		@Override
		public void close() throws Exception {
			shutdown().join();
			Assert.assertTrue(isEventGroupShutdown());
		}
	}

	/**
	 * A server that receives a {@link TestMessage test message} and returns another test
	 * message containing the same string as the request with the name of the server prepended.
	 */
	private static class TestServer extends AbstractServerBase<TestMessage, TestMessage> implements AutoCloseable {

		private final KvStateRequestStats requestStats;

		TestServer(String name, KvStateRequestStats stats, Iterator<Integer> bindPort) throws UnknownHostException {
			super(name, InetAddress.getLocalHost(), bindPort, 1, 1);
			this.requestStats = stats;
		}

		@Override
		public AbstractServerHandler<TestMessage, TestMessage> initializeHandler() {
			return new AbstractServerHandler<TestMessage, TestMessage>(
					this,
					new MessageSerializer<>(new TestMessage.TestMessageDeserializer(), new TestMessage.TestMessageDeserializer()),
					requestStats) {

				@Override
				public CompletableFuture<TestMessage> handleRequest(long requestId, TestMessage request) {
					TestMessage response = new TestMessage(getServerName() + '-' + request.getMessage());
					return CompletableFuture.completedFuture(response);
				}

				@Override
				public CompletableFuture<Void> shutdown() {
					return CompletableFuture.completedFuture(null);
				}
			};
		}

		@Override
		public void close() throws Exception {
			shutdownServer().get();
			if (requestStats instanceof AtomicKvStateRequestStats) {
				AtomicKvStateRequestStats stats = (AtomicKvStateRequestStats) requestStats;
				Assert.assertEquals(0L, stats.getNumConnections());
			}
			Assert.assertTrue(getQueryExecutor().isTerminated());
			Assert.assertTrue(isEventGroupShutdown());
		}
	}

	/**
	 * Message with a string as payload.
	 */
	private static class TestMessage extends MessageBody {

		private final String message;

		TestMessage(String message) {
			this.message = Preconditions.checkNotNull(message);
		}

		public String getMessage() {
			return message;
		}

		@Override
		public byte[] serialize() {
			byte[] content = message.getBytes(ConfigConstants.DEFAULT_CHARSET);

			// message size + 4 for the length itself
			return ByteBuffer.allocate(content.length + Integer.BYTES)
					.putInt(content.length)
					.put(content)
					.array();
		}

		/**
		 * The deserializer for our {@link TestMessage test messages}.
		 */
		public static class TestMessageDeserializer implements MessageDeserializer<TestMessage> {

			@Override
			public TestMessage deserializeMessage(ByteBuf buf) {
				int length = buf.readInt();
				String message = "";
				if (length > 0) {
					byte[] name = new byte[length];
					buf.readBytes(name);
					message = new String(name, ConfigConstants.DEFAULT_CHARSET);
				}
				return new TestMessage(message);
			}
		}
	}
}
