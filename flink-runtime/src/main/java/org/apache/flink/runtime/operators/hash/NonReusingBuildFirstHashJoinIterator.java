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


package org.apache.flink.runtime.operators.hash;

import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.api.common.typeutils.TypePairComparator;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.io.disk.iomanager.IOManager;
import org.apache.flink.runtime.jobgraph.tasks.AbstractInvokable;
import org.apache.flink.runtime.memory.MemoryAllocationException;
import org.apache.flink.runtime.memory.MemoryManager;
import org.apache.flink.runtime.operators.util.JoinTaskIterator;
import org.apache.flink.util.Collector;
import org.apache.flink.util.MutableObjectIterator;

import java.io.IOException;
import java.util.List;


/**
 * An implementation of the {@link org.apache.flink.runtime.operators.util.JoinTaskIterator} that uses a hybrid-hash-join
 * internally to match the records with equal key. The build side of the hash is the first input of the match.
 * This implementation DOES NOT reuse objects.
 */
public class NonReusingBuildFirstHashJoinIterator<V1, V2, O> extends HashJoinIteratorBase implements JoinTaskIterator<V1, V2, O> {

	protected final MutableHashTable<V1, V2> hashJoin;

	protected final TypeSerializer<V2> probeSideSerializer;

	private final MemoryManager memManager;

	private final MutableObjectIterator<V1> firstInput;

	private final MutableObjectIterator<V2> secondInput;

	private final boolean probeSideOuterJoin;

	private final boolean buildSideOuterJoin;

	private volatile boolean running = true;

	// --------------------------------------------------------------------------------------------

	public NonReusingBuildFirstHashJoinIterator(
			MutableObjectIterator<V1> firstInput,
			MutableObjectIterator<V2> secondInput,
			TypeSerializer<V1> serializer1,
			TypeComparator<V1> comparator1,
			TypeSerializer<V2> serializer2,
			TypeComparator<V2> comparator2,
			TypePairComparator<V2, V1> pairComparator,
			MemoryManager memManager, IOManager ioManager,
			AbstractInvokable ownerTask,
			double memoryFraction,
			boolean probeSideOuterJoin,
			boolean buildSideOuterJoin,
			boolean useBitmapFilters) throws MemoryAllocationException {
		
		this.memManager = memManager;
		this.firstInput = firstInput;
		this.secondInput = secondInput;
		this.probeSideSerializer = serializer2;

		if(useBitmapFilters && probeSideOuterJoin) {
			throw new IllegalArgumentException("Bitmap filter may not be activated for joining with empty build side");
		}
		this.probeSideOuterJoin = probeSideOuterJoin;
		this.buildSideOuterJoin = buildSideOuterJoin;

		this.hashJoin = getHashJoin(serializer1, comparator1, serializer2, comparator2,
				pairComparator, memManager, ioManager, ownerTask, memoryFraction, useBitmapFilters);
	}
	
	// --------------------------------------------------------------------------------------------
	
	@Override
	public void open() throws IOException, MemoryAllocationException, InterruptedException {
		this.hashJoin.open(this.firstInput, this.secondInput, this.buildSideOuterJoin);
	}
	

	@Override
	public void close() {
		// close the join
		this.hashJoin.close();
		
		// free the memory
		final List<MemorySegment> segments = this.hashJoin.getFreedMemory();
		this.memManager.release(segments);
	}

	@Override
	public final boolean callWithNextKey(FlatJoinFunction<V1, V2, O> matchFunction, Collector<O> collector)
	throws Exception
	{
		if (this.hashJoin.nextRecord())
		{
			// we have a next record, get the iterators to the probe and build side values
			final MutableObjectIterator<V1> buildSideIterator = this.hashJoin.getBuildSideIterator();
			final V2 probeRecord = this.hashJoin.getCurrentProbeRecord();
			V1 nextBuildSideRecord = buildSideIterator.next();

			// get the first build side value
			if (probeRecord != null && nextBuildSideRecord != null) {
				V1 tmpRec;

				// check if there is another build-side value
				if ((tmpRec = buildSideIterator.next()) != null) {
					// more than one build-side value --> copy the probe side
					V2 probeCopy;
					probeCopy = this.probeSideSerializer.copy(probeRecord);
					
					// call match on the first pair
					matchFunction.join(nextBuildSideRecord, probeCopy, collector);
					
					// call match on the second pair
					probeCopy = this.probeSideSerializer.copy(probeRecord);
					matchFunction.join(tmpRec, probeCopy, collector);
					
					while (this.running && ((nextBuildSideRecord = buildSideIterator.next()) != null)) {
						// call match on the next pair
						// make sure we restore the value of the probe side record
						probeCopy = this.probeSideSerializer.copy(probeRecord);
						matchFunction.join(nextBuildSideRecord, probeCopy, collector);
					}
				}
				else {
					// only single pair matches
					matchFunction.join(nextBuildSideRecord, probeRecord, collector);
				}
			} else {
				// while probe side outer join, join current probe record with null.
				if (probeSideOuterJoin && probeRecord != null && nextBuildSideRecord == null) {
					matchFunction.join(null, probeRecord, collector);
				}

				// while build side outer join, iterate all build records which have not been probed before,
				// and join with null.
				if (buildSideOuterJoin && probeRecord == null && nextBuildSideRecord != null) {
					matchFunction.join(nextBuildSideRecord, null, collector);

					while (this.running && ((nextBuildSideRecord = buildSideIterator.next()) != null)) {
						matchFunction.join(nextBuildSideRecord, null, collector);
					}
				}
			}

			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void abort() {
		this.running = false;
		this.hashJoin.abort();
	}
}
