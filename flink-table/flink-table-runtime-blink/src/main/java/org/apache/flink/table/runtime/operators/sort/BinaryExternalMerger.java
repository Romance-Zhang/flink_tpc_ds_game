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

package org.apache.flink.table.runtime.operators.sort;

import org.apache.flink.runtime.io.disk.ChannelReaderInputViewIterator;
import org.apache.flink.runtime.io.disk.iomanager.AbstractChannelReaderInputView;
import org.apache.flink.runtime.io.disk.iomanager.IOManager;
import org.apache.flink.runtime.memory.AbstractPagedOutputView;
import org.apache.flink.table.dataformat.BinaryRow;
import org.apache.flink.table.runtime.compression.BlockCompressionFactory;
import org.apache.flink.table.runtime.generated.RecordComparator;
import org.apache.flink.table.runtime.typeutils.BinaryRowSerializer;
import org.apache.flink.util.MutableObjectIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Record merger for sort of BinaryRow.
 */
public class BinaryExternalMerger extends AbstractBinaryExternalMerger<BinaryRow> {

	private final BinaryRowSerializer serializer;
	private final RecordComparator comparator;

	public BinaryExternalMerger(
			IOManager ioManager,
			int pageSize,
			int maxFanIn,
			SpillChannelManager channelManager,
			BinaryRowSerializer serializer,
			RecordComparator comparator,
			boolean compressionEnable,
			BlockCompressionFactory compressionCodecFactory,
			int compressionBlockSize) {
		super(ioManager, pageSize, maxFanIn, channelManager, compressionEnable, compressionCodecFactory, compressionBlockSize);
		this.serializer = serializer;
		this.comparator = comparator;
	}

	@Override
	protected MutableObjectIterator<BinaryRow> channelReaderInputViewIterator(AbstractChannelReaderInputView inView) {
		return new ChannelReaderInputViewIterator<>(inView, null, serializer.duplicate());
	}

	@Override
	protected Comparator<BinaryRow> mergeComparator() {
		return comparator::compare;
	}

	@Override
	protected List<BinaryRow> mergeReusedEntries(int size) {
		ArrayList<BinaryRow> reused = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			reused.add(serializer.createInstance());
		}
		return reused;
	}

	@Override
	protected void writeMergingOutput(
			MutableObjectIterator<BinaryRow> mergeIterator,
			AbstractPagedOutputView output) throws IOException {
		// read the merged stream and write the data back
		BinaryRow rec = serializer.createInstance();
		while ((rec = mergeIterator.next(rec)) != null) {
			serializer.serialize(rec, output);
		}
	}
}
