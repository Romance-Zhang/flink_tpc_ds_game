/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.tests;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This source function generates a sequence of long values per key.
 */
public class SequenceGeneratorSource extends RichParallelSourceFunction<Event> implements CheckpointedFunction {

	private static final long serialVersionUID = -3986989644799442178L;

	/** Length of the artificial payload string generated for each event. */
	private final int payloadLength;

	/** The size of the total key space, i.e. the number of unique keys generated by all parallel sources. */
	private final int totalKeySpaceSize;

	/** This determines how much the event time progresses for each generated element. */
	private final long eventTimeClockProgressPerEvent;

	/** Maximum generated deviation in event time from the current event time clock. */
	private final long maxOutOfOrder;

	/** Time that a sleep takes in milliseconds. A value < 1 deactivates sleeping. */
	private final long sleepTime;

	/** This determines after how many generated events we sleep. A value < 1 deactivates sleeping. */
	private final long sleepAfterElements;

	/** The current event time progress of this source; will start from 0. */
	private long monotonousEventTime;

	/** This holds the key ranges for which this source generates events. */
	private transient List<KeyRangeStates> keyRanges;

	/** This is used to snapshot the state of this source, one entry per key range. */
	private transient ListState<KeyRangeStates> snapshotKeyRanges;

	/** This is used to snapshot the event time progress of the sources. */
	private transient ListState<Long> lastEventTimes;

	/** Flag that determines if this source is running, i.e. generating events. */
	private volatile boolean running;

	SequenceGeneratorSource(
		int totalKeySpaceSize,
		int payloadLength,
		long maxOutOfOrder,
		long eventTimeClockProgressPerEvent,
		long sleepTime,
		long sleepAfterElements) {

		this.totalKeySpaceSize = totalKeySpaceSize;
		this.maxOutOfOrder = maxOutOfOrder;
		this.payloadLength = payloadLength;
		this.eventTimeClockProgressPerEvent = eventTimeClockProgressPerEvent;
		this.sleepTime = sleepTime;
		this.sleepAfterElements = sleepTime > 0 ? sleepAfterElements : 0;
		this.running = true;
	}

	@Override
	public void run(SourceContext<Event> ctx) throws Exception {
		if (keyRanges.size() > 0) {
			runActive(ctx);
		} else {
			runIdle(ctx);
		}
	}

	private void runActive(SourceContext<Event> ctx) throws Exception {
		Random random = new Random();

		// this holds the current event time, from which generated events can up to +/- (maxOutOfOrder).
		long elementsBeforeSleep = sleepAfterElements;

		while (running) {

			KeyRangeStates randomKeyRangeStates = keyRanges.get(random.nextInt(keyRanges.size()));
			int randomKey = randomKeyRangeStates.getRandomKey(random);

			long eventTime = Math.max(
				0,
				generateEventTimeWithOutOfOrderness(random, monotonousEventTime));

			// uptick the event time clock
			monotonousEventTime += eventTimeClockProgressPerEvent;

			synchronized (ctx.getCheckpointLock()) {
				long value = randomKeyRangeStates.incrementAndGet(randomKey);

				Event event = new Event(
					randomKey,
					eventTime,
					value,
					StringUtils.getRandomString(random, payloadLength, payloadLength, 'A', 'z'));

				ctx.collect(event);
			}

			if (sleepTime > 0) {
				if (elementsBeforeSleep == 1) {
					elementsBeforeSleep = sleepAfterElements;
					Thread.sleep(sleepTime);
				} else if (elementsBeforeSleep > 1) {
					--elementsBeforeSleep;
				}
			}
		}
	}

	private void runIdle(SourceContext<Event> ctx) {
		ctx.markAsTemporarilyIdle();

		// just wait until this source is canceled
		final Object waitLock = new Object();
		while (running) {
			try {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (waitLock) {
					waitLock.wait();
				}
			}
			catch (InterruptedException e) {
				if (!running) {
					// restore the interrupted state, and fall through the loop
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private long generateEventTimeWithOutOfOrderness(Random random, long correctTime) {
		if (maxOutOfOrder > 0) {
			return correctTime - maxOutOfOrder + ((random.nextLong() & Long.MAX_VALUE) % (2 * maxOutOfOrder));
		} else {
			return correctTime;
		}
	}

	@Override
	public void cancel() {
		running = false;
	}

	@Override
	public void snapshotState(FunctionSnapshotContext context) throws Exception {
		snapshotKeyRanges.update(keyRanges);

		lastEventTimes.clear();
		lastEventTimes.add(monotonousEventTime);
	}

	@Override
	public void initializeState(FunctionInitializationContext context) throws Exception {
		final RuntimeContext runtimeContext = getRuntimeContext();
		final int subtaskIdx = runtimeContext.getIndexOfThisSubtask();
		final int parallelism = runtimeContext.getNumberOfParallelSubtasks();
		final int maxParallelism = runtimeContext.getMaxNumberOfParallelSubtasks();

		ListStateDescriptor<Long> unionWatermarksStateDescriptor =
			new ListStateDescriptor<>("watermarks", Long.class);

		lastEventTimes = context.getOperatorStateStore().getUnionListState(unionWatermarksStateDescriptor);

		ListStateDescriptor<KeyRangeStates> stateDescriptor =
			new ListStateDescriptor<>("keyRanges", KeyRangeStates.class);

		snapshotKeyRanges = context.getOperatorStateStore().getListState(stateDescriptor);
		keyRanges = new ArrayList<>();

		if (context.isRestored()) {
			// restore key ranges from the snapshot
			for (KeyRangeStates keyRange : snapshotKeyRanges.get()) {
				keyRanges.add(keyRange);
			}

			// let event time start from the max of all event time progress across subtasks in the last execution
			for (Long lastEventTime : lastEventTimes.get()) {
				monotonousEventTime = Math.max(monotonousEventTime, lastEventTime);
			}
		} else {
			// determine the key ranges that belong to the subtask
			int rangeStartIdx = (subtaskIdx * maxParallelism) / parallelism;
			int rangeEndIdx = ((subtaskIdx + 1) * maxParallelism) / parallelism;

			for (int i = rangeStartIdx; i < rangeEndIdx; ++i) {

				int start = ((i * totalKeySpaceSize + maxParallelism - 1) / maxParallelism);
				int end = 1 + ((i + 1) * totalKeySpaceSize - 1) / maxParallelism;

				if (end - start > 0) {
					keyRanges.add(new KeyRangeStates(start, end));
				}
			}

			// fresh run; start from event time o
			monotonousEventTime = 0L;
		}
	}

	/**
	 * This defines the key-range and the current sequence numbers for all keys in the range.
	 */
	private static class KeyRangeStates {

		/** Start key of the range (inclusive). */
		final int startKey;

		/** Start key of the range (exclusive). */
		final int endKey;

		/** This array contains the current sequence number for each key in the range. */
		final long[] statesPerKey;

		KeyRangeStates(int startKey, int endKey) {
			this(startKey, endKey, new long[endKey - startKey]);
		}

		KeyRangeStates(int startKey, int endKey, long[] statesPerKey) {
			Preconditions.checkArgument(statesPerKey.length == endKey - startKey);
			this.startKey = startKey;
			this.endKey = endKey;
			this.statesPerKey = statesPerKey;
		}

		/**
		 * Increments and returns the current sequence number for the given key.
		 */
		long incrementAndGet(int key) {
			return ++statesPerKey[key - startKey];
		}

		/**
		 * Returns a random key that belongs to this key range.
		 */
		int getRandomKey(Random random) {
			return random.nextInt(endKey - startKey) + startKey;
		}

		@Override
		public String toString() {
			return "KeyRangeStates{" +
				"start=" + startKey +
				", end=" + endKey +
				", statesPerKey=" + Arrays.toString(statesPerKey) +
				'}';
		}
	}
}
