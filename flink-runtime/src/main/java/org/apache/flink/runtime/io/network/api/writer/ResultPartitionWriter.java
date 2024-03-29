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

package org.apache.flink.runtime.io.network.api.writer;

import org.apache.flink.runtime.io.network.buffer.BufferBuilder;
import org.apache.flink.runtime.io.network.buffer.BufferConsumer;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;

import javax.annotation.Nullable;

import java.io.IOException;

/**
 * A buffer-oriented runtime result writer API for producing results.
 *
 * <p>If {@link ResultPartitionWriter#close()} is called before {@link ResultPartitionWriter#fail(Throwable)} or
 * {@link ResultPartitionWriter#finish()}, it abruptly triggers failure and cancellation of production.
 * In this case {@link ResultPartitionWriter#fail(Throwable)} still needs to be called afterwards to fully release
 * all resources associated the the partition and propagate failure cause to the consumer if possible.
 */
public interface ResultPartitionWriter extends AutoCloseable {

	/**
	 * Setup partition, potentially heavy-weight, blocking operation comparing to just creation.
	 */
	void setup() throws IOException;

	ResultPartitionID getPartitionId();

	int getNumberOfSubpartitions();

	int getNumTargetKeyGroups();

	/**
	 * Requests a {@link BufferBuilder} from this partition for writing data.
	 */
	BufferBuilder getBufferBuilder() throws IOException, InterruptedException;

	/**
	 * Adds the bufferConsumer to the subpartition with the given index.
	 *
	 * <p>This method takes the ownership of the passed {@code bufferConsumer} and thus is responsible for releasing
	 * it's resources.
	 *
	 * <p>To avoid problems with data re-ordering, before adding new {@link BufferConsumer} the previously added one
	 * the given {@code subpartitionIndex} must be marked as {@link BufferConsumer#isFinished()}.
	 *
	 * @return true if operation succeeded and bufferConsumer was enqueued for consumption.
	 */
	boolean addBufferConsumer(BufferConsumer bufferConsumer, int subpartitionIndex) throws IOException;

	/**
	 * Manually trigger consumption from enqueued {@link BufferConsumer BufferConsumers} in all subpartitions.
	 */
	void flushAll();

	/**
	 * Manually trigger consumption from enqueued {@link BufferConsumer BufferConsumers} in one specified subpartition.
	 */
	void flush(int subpartitionIndex);

	/**
	 * Fail the production of the partition.
	 *
	 * <p>This method propagates non-{@code null} failure causes to consumers on a best-effort basis. This call also
	 * leads to the release of all resources associated with the partition. Closing of the partition is still needed
	 * afterwards if it has not been done before.
	 *
	 * @param throwable failure cause
	 */
	void fail(@Nullable Throwable throwable);

	/**
	 * Successfully finish the production of the partition.
	 *
	 * <p>Closing of partition is still needed afterwards.
	 */
	void finish() throws IOException;
}
