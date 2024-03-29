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

package org.apache.flink.runtime.io.disk.iomanager;

import org.apache.flink.runtime.io.network.buffer.Buffer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousBufferFileReader extends AsynchronousFileIOChannel<Buffer, ReadRequest> implements BufferFileReader {

	private final AtomicBoolean hasReachedEndOfFile = new AtomicBoolean();

	protected AsynchronousBufferFileReader(ID channelID, RequestQueue<ReadRequest> requestQueue, RequestDoneCallback<Buffer> callback) throws IOException {
		super(channelID, requestQueue, callback, false);
	}

	@Override
	public void readInto(Buffer buffer) throws IOException {
		addRequest(new BufferReadRequest(this, buffer, hasReachedEndOfFile));
	}

	@Override
	public void seekToPosition(long position) throws IOException {
		requestQueue.add(new SeekRequest(this, position));
	}

	@Override
	public boolean hasReachedEndOfFile() {
		return hasReachedEndOfFile.get();
	}
}
