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

package org.apache.flink.api.common.typeutils.base;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.types.LongValue;
import org.apache.flink.types.NormalizableKey;

import java.io.IOException;

/**
 * Specialized comparator for LongValue based on CopyableValueComparator.
 */
@Internal
public class LongValueComparator extends TypeComparator<LongValue> {

	private static final long serialVersionUID = 1L;

	private final boolean ascendingComparison;

	private final LongValue reference = new LongValue();

	private final LongValue tempReference = new LongValue();

	private final TypeComparator<?>[] comparators = new TypeComparator[] {this};

	public LongValueComparator(boolean ascending) {
		this.ascendingComparison = ascending;
	}

	@Override
	public int hash(LongValue record) {
		return record.hashCode();
	}

	@Override
	public void setReference(LongValue toCompare) {
		toCompare.copyTo(reference);
	}

	@Override
	public boolean equalToReference(LongValue candidate) {
		return candidate.equals(this.reference);
	}

	@Override
	public int compareToReference(TypeComparator<LongValue> referencedComparator) {
		LongValue otherRef = ((LongValueComparator) referencedComparator).reference;
		int comp = otherRef.compareTo(reference);
		return ascendingComparison ? comp : -comp;
	}

	@Override
	public int compare(LongValue first, LongValue second) {
		int comp = first.compareTo(second);
		return ascendingComparison ? comp : -comp;
	}

	@Override
	public int compareSerialized(DataInputView firstSource, DataInputView secondSource) throws IOException {
		reference.read(firstSource);
		tempReference.read(secondSource);
		int comp = reference.compareTo(tempReference);
		return ascendingComparison ? comp : -comp;
	}

	@Override
	public boolean supportsNormalizedKey() {
		return NormalizableKey.class.isAssignableFrom(LongValue.class);
	}

	@Override
	public int getNormalizeKeyLen() {
		return reference.getMaxNormalizedKeyLen();
	}

	@Override
	public boolean isNormalizedKeyPrefixOnly(int keyBytes) {
		return keyBytes < getNormalizeKeyLen();
	}

	@Override
	public void putNormalizedKey(LongValue record, MemorySegment target, int offset, int numBytes) {
		record.copyNormalizedKey(target, offset, numBytes);
	}

	@Override
	public boolean invertNormalizedKey() {
		return !ascendingComparison;
	}

	@Override
	public TypeComparator<LongValue> duplicate() {
		return new LongValueComparator(ascendingComparison);
	}

	@Override
	public int extractKeys(Object record, Object[] target, int index) {
		target[index] = record;
		return 1;
	}

	@Override
	public TypeComparator<?>[] getFlatComparators() {
		return comparators;
	}

	// --------------------------------------------------------------------------------------------
	// unsupported normalization
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean supportsSerializationWithKeyNormalization() {
		return false;
	}

	@Override
	public void writeWithKeyNormalization(LongValue record, DataOutputView target) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public LongValue readWithKeyDenormalization(LongValue reuse, DataInputView source) throws IOException {
		throw new UnsupportedOperationException();
	}
}
