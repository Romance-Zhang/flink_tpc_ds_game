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

package org.apache.flink.table.runtime.operators.join.temporal;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.streaming.api.operators.InternalTimer;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.dataformat.BaseRow;
import org.apache.flink.table.dataformat.JoinedRow;
import org.apache.flink.table.dataformat.util.BaseRowUtil;
import org.apache.flink.table.runtime.generated.GeneratedJoinCondition;
import org.apache.flink.table.runtime.generated.JoinCondition;
import org.apache.flink.table.runtime.typeutils.BaseRowTypeInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

/**
 * This operator works by keeping on the state collection of probe and build records to process
 * on next watermark. The idea is that between watermarks we are collecting those elements
 * and once we are sure that there will be no updates we emit the correct result and clean up the
 * state.
 *
 * <p>Cleaning up the state drops all of the "old" values from the probe side, where "old" is defined
 * as older then the current watermark. Build side is also cleaned up in the similar fashion,
 * however we always keep at least one record - the latest one - even if it's past the last
 * watermark.
 *
 * <p>One more trick is how the emitting results and cleaning up is triggered. It is achieved
 * by registering timers for the keys. We could register a timer for every probe and build
 * side element's event time (when watermark exceeds this timer, that's when we are emitting and/or
 * cleaning up the state). However this would cause huge number of registered timers. For example
 * with following evenTimes of probe records accumulated: {1, 2, 5, 8, 9}, if we
 * had received Watermark(10), it would trigger 5 separate timers for the same key. To avoid that
 * we always keep only one single registered timer for any given key, registered for the minimal
 * value. Upon triggering it, we process all records with event times older then or equal to
 * currentWatermark.
 */
public class TemporalRowTimeJoinOperator
	extends BaseTwoInputStreamOperatorWithStateRetention {

	private static final long serialVersionUID = 6642514795175288193L;

	private static final String NEXT_LEFT_INDEX_STATE_NAME = "next-index";
	private static final String LEFT_STATE_NAME = "left";
	private static final String RIGHT_STATE_NAME = "right";
	private static final String REGISTERED_TIMER_STATE_NAME = "timer";
	private static final String TIMERS_STATE_NAME = "timers";

	private final BaseRowTypeInfo leftType;
	private final BaseRowTypeInfo rightType;
	private final GeneratedJoinCondition generatedJoinCondition;
	private final int leftTimeAttribute;
	private final int rightTimeAttribute;

	private final RowtimeComparator rightRowtimeComparator;

	/**
	 * Incremental index generator for {@link #leftState}'s keys.
	 */
	private transient ValueState<Long> nextLeftIndex;

	/**
	 * Mapping from artificial row index (generated by `nextLeftIndex`) into the left side `Row`.
	 * We can not use List to accumulate Rows, because we need efficient deletes of the oldest rows.
	 *
	 * <p>TODO: this could be OrderedMultiMap[Jlong, Row] indexed by row's timestamp, to avoid
	 * full map traversals (if we have lots of rows on the state that exceed `currentWatermark`).
	 */
	private transient MapState<Long, BaseRow> leftState;

	/**
	 * Mapping from timestamp to right side `Row`.
	 *
	 * <p>TODO: having `rightState` as an OrderedMapState would allow us to avoid sorting cost
	 * once per watermark
	 */
	private transient MapState<Long, BaseRow> rightState;

	// Long for correct handling of default null
	private transient ValueState<Long> registeredTimer;
	private transient TimestampedCollector<BaseRow> collector;
	private transient InternalTimerService<VoidNamespace> timerService;

	private transient JoinCondition joinCondition;
	private transient JoinedRow outRow;

	public TemporalRowTimeJoinOperator(
			BaseRowTypeInfo leftType,
			BaseRowTypeInfo rightType,
			GeneratedJoinCondition generatedJoinCondition,
			int leftTimeAttribute,
			int rightTimeAttribute,
			long minRetentionTime,
			long maxRetentionTime) {
		super(minRetentionTime, maxRetentionTime);
		this.leftType = leftType;
		this.rightType = rightType;
		this.generatedJoinCondition = generatedJoinCondition;
		this.leftTimeAttribute = leftTimeAttribute;
		this.rightTimeAttribute = rightTimeAttribute;
		this.rightRowtimeComparator = new RowtimeComparator(rightTimeAttribute);
	}

	@Override
	public void open() throws Exception {
		joinCondition = generatedJoinCondition.newInstance(getRuntimeContext().getUserCodeClassLoader());
		joinCondition.setRuntimeContext(getRuntimeContext());
		joinCondition.open(new Configuration());

		nextLeftIndex = getRuntimeContext().getState(
			new ValueStateDescriptor<>(NEXT_LEFT_INDEX_STATE_NAME, Types.LONG));
		leftState = getRuntimeContext().getMapState(
			new MapStateDescriptor<>(LEFT_STATE_NAME, Types.LONG, leftType));
		rightState = getRuntimeContext().getMapState(
			new MapStateDescriptor<>(RIGHT_STATE_NAME, Types.LONG, rightType));
		registeredTimer = getRuntimeContext().getState(
			new ValueStateDescriptor<>(REGISTERED_TIMER_STATE_NAME, Types.LONG));

		timerService = getInternalTimerService(
			TIMERS_STATE_NAME, VoidNamespaceSerializer.INSTANCE, this);
		collector = new TimestampedCollector<>(output);
		outRow = new JoinedRow();
		outRow.setHeader(BaseRowUtil.ACCUMULATE_MSG);
	}

	@Override
	public void processElement1(StreamRecord<BaseRow> element) throws Exception {
		BaseRow row = element.getValue();
		checkNotRetraction(row);

		leftState.put(getNextLeftIndex(), row);
		registerSmallestTimer(getLeftTime(row)); // Timer to emit and clean up the state

		registerProcessingCleanupTimer();
	}

	@Override
	public void processElement2(StreamRecord<BaseRow> element) throws Exception {
		BaseRow row = element.getValue();
		checkNotRetraction(row);

		long rowTime = getRightTime(row);
		rightState.put(rowTime, row);
		registerSmallestTimer(rowTime); // Timer to clean up the state

		registerProcessingCleanupTimer();
	}

	@Override
	public void onEventTime(InternalTimer<Object, VoidNamespace> timer) throws Exception {
		registeredTimer.clear();
		long lastUnprocessedTime = emitResultAndCleanUpState(timerService.currentWatermark());
		if (lastUnprocessedTime < Long.MAX_VALUE) {
			registerTimer(lastUnprocessedTime);
		}

		// if we have more state at any side, then update the timer, else clean it up.
		if (stateCleaningEnabled) {
			if (lastUnprocessedTime < Long.MAX_VALUE || rightState.iterator().hasNext()) {
				registerProcessingCleanupTimer();
			} else {
				cleanupLastTimer();
			}
		}
	}

	@Override
	public void close() throws Exception {
		if (joinCondition != null) {
			joinCondition.close();
		}
	}

	/**
	 * @return a row time of the oldest unprocessed probe record or Long.MaxValue, if all records
	 *         have been processed.
	 */
	private long emitResultAndCleanUpState(long timerTimestamp) throws Exception {
		List<BaseRow> rightRowsSorted = getRightRowSorted(rightRowtimeComparator);
		long lastUnprocessedTime = Long.MAX_VALUE;

		Iterator<Map.Entry<Long, BaseRow>> leftIterator = leftState.entries().iterator();
		while (leftIterator.hasNext()) {
			Map.Entry<Long, BaseRow> entry = leftIterator.next();
			BaseRow leftRow = entry.getValue();
			long leftTime = getLeftTime(leftRow);

			if (leftTime <= timerTimestamp) {
				Optional<BaseRow> rightRow = latestRightRowToJoin(rightRowsSorted, leftTime);
				if (rightRow.isPresent()) {
					if (joinCondition.apply(leftRow, rightRow.get())) {
						outRow.replace(leftRow, rightRow.get());
						collector.collect(outRow);
					}
				}
				leftIterator.remove();
			} else {
				lastUnprocessedTime = Math.min(lastUnprocessedTime, leftTime);
			}
		}

		cleanupState(timerTimestamp, rightRowsSorted);
		return lastUnprocessedTime;
	}

	/**
	 * Removes all right entries older then the watermark, except the latest one. For example with:
	 * rightState = [1, 5, 9]
	 * and
	 * watermark = 6
	 * we can not remove "5" from rightState, because left elements with rowtime of 7 or 8 could
	 * be joined with it later
	 */
	private void cleanupState(long timerTimestamp, List<BaseRow> rightRowsSorted) throws Exception {
		int i = 0;
		int indexToKeep = firstIndexToKeep(timerTimestamp, rightRowsSorted);
		while (i < indexToKeep) {
			long rightTime = getRightTime(rightRowsSorted.get(i));
			rightState.remove(rightTime);
			i += 1;
		}
	}

	/**
	 * The method to be called when a cleanup timer fires.
	 *
	 * @param time The timestamp of the fired timer.
	 */
	@Override
	public void cleanupState(long time) {
		leftState.clear();
		rightState.clear();
	}

	private int firstIndexToKeep(long timerTimestamp, List<BaseRow> rightRowsSorted) {
		int firstIndexNewerThenTimer =
			indexOfFirstElementNewerThanTimer(timerTimestamp, rightRowsSorted);

		if (firstIndexNewerThenTimer < 0) {
			return rightRowsSorted.size() - 1;
		}
		else {
			return firstIndexNewerThenTimer - 1;
		}
	}

	private int indexOfFirstElementNewerThanTimer(long timerTimestamp, List<BaseRow> list) {
		ListIterator<BaseRow> iter = list.listIterator();
		while (iter.hasNext()) {
			if (getRightTime(iter.next()) > timerTimestamp) {
				return iter.previousIndex();
			}
		}
		return -1;
	}

	/**
	 * Binary search {@code rightRowsSorted} to find the latest right row to join with {@code leftTime}.
	 * Latest means a right row with largest time that is still smaller or equal to {@code leftTime}.
	 *
	 * @return found element or {@code Optional.empty} If such row was not found (either {@code rightRowsSorted}
	 *         is empty or all {@code rightRowsSorted} are are newer).
	 */
	private Optional<BaseRow> latestRightRowToJoin(List<BaseRow> rightRowsSorted, long leftTime) {
		return latestRightRowToJoin(rightRowsSorted, 0, rightRowsSorted.size() - 1, leftTime);
	}

	private Optional<BaseRow> latestRightRowToJoin(
			List<BaseRow> rightRowsSorted,
			int low,
			int high,
			long leftTime) {
		if (low > high) {
			// exact value not found, we are returning largest from the values smaller then leftTime
			if (low - 1 < 0) {
				return Optional.empty();
			}
			else {
				return Optional.of(rightRowsSorted.get(low - 1));
			}
		} else {
			int mid = (low + high) >>> 1;
			BaseRow midRow = rightRowsSorted.get(mid);
			long midTime = getRightTime(midRow);
			int cmp = Long.compare(midTime, leftTime);
			if (cmp < 0) {
				return latestRightRowToJoin(rightRowsSorted, mid + 1, high, leftTime);
			}
			else if (cmp > 0) {
				return latestRightRowToJoin(rightRowsSorted, low, mid - 1, leftTime);
			}
			else {
				return Optional.of(midRow);
			}
		}
	}

	private void registerSmallestTimer(long timestamp) throws IOException {
		Long currentRegisteredTimer = registeredTimer.value();
		if (currentRegisteredTimer == null) {
			registerTimer(timestamp);
		} else if (currentRegisteredTimer > timestamp) {
			timerService.deleteEventTimeTimer(VoidNamespace.INSTANCE, currentRegisteredTimer);
			registerTimer(timestamp);
		}
	}

	private void registerTimer(long timestamp) throws IOException {
		registeredTimer.update(timestamp);
		timerService.registerEventTimeTimer(VoidNamespace.INSTANCE, timestamp);
	}

	private List<BaseRow> getRightRowSorted(RowtimeComparator rowtimeComparator) throws Exception {
		List<BaseRow> rightRows = new ArrayList<>();
		for (BaseRow row : rightState.values()) {
			rightRows.add(row);
		}
		rightRows.sort(rowtimeComparator);
		return rightRows;
	}

	private long getNextLeftIndex() throws IOException {
		Long index = nextLeftIndex.value();
		if (index == null) {
			index = 0L;
		}
		nextLeftIndex.update(index + 1);
		return index;
	}

	private long getLeftTime(BaseRow leftRow) {
		return leftRow.getLong(leftTimeAttribute);
	}

	private long getRightTime(BaseRow rightRow) {
		return rightRow.getLong(rightTimeAttribute);
	}

	private void checkNotRetraction(BaseRow row) {
		if (BaseRowUtil.isRetractMsg(row)) {
			String className = getClass().getSimpleName();
			throw new IllegalStateException(
				"Retractions are not supported by " + className +
					". If this can happen it should be validated during planning!");
		}
	}

	// ------------------------------------------------------------------------------------------

	private static class RowtimeComparator implements Comparator<BaseRow>, Serializable {

		private static final long serialVersionUID = 8160134014590716914L;

		private final int timeAttribute;

		private RowtimeComparator(int timeAttribute) {
			this.timeAttribute = timeAttribute;
		}

		@Override
		public int compare(BaseRow o1, BaseRow o2) {
			long o1Time = o1.getLong(timeAttribute);
			long o2Time = o2.getLong(timeAttribute);
			return Long.compare(o1Time, o2Time);
		}
	}
}
