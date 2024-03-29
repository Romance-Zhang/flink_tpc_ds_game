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

package org.apache.flink.table.types.logical;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Logical type of a variable-length binary string (=a sequence of bytes).
 *
 * <p>The serialized string representation is {@code VARBINARY(n)} where {@code n} is the maximum
 * number of bytes. {@code n} must have a value between 1 and {@link Integer#MAX_VALUE} (both
 * inclusive). If no length is specified, {@code n} is equal to 1. {@code BYTES} is a synonym for
 * {@code VARBINARY(2147483647)}.
 *
 * <p>For enabling type inference of a zero-length binary string literal to a variable-length binary
 * string, this type does also support {@code n} to be 0. However, this is not exposed through the API.
 */
@PublicEvolving
public final class VarBinaryType extends LogicalType {

	public static final int EMPTY_LITERAL_LENGTH = 0;

	public static final int MIN_LENGTH = 1;

	public static final int MAX_LENGTH = Integer.MAX_VALUE;

	public static final int DEFAULT_LENGTH = 1;

	private static final String FORMAT = "VARBINARY(%d)";

	private static final String MAX_FORMAT = "BYTES";

	private static final Set<String> INPUT_OUTPUT_CONVERSION = conversionSet(
		byte[].class.getName(),
		"org.apache.flink.table.dataformat.BinaryArray");

	private static final Class<?> DEFAULT_CONVERSION = byte[].class;

	private final int length;

	public VarBinaryType(boolean isNullable, int length) {
		super(isNullable, LogicalTypeRoot.VARBINARY);
		if (length < MIN_LENGTH) {
			throw new ValidationException(
				String.format(
					"Variable binary string length must be between %d and %d (both inclusive).",
					MIN_LENGTH,
					MAX_LENGTH));
		}
		this.length = length;
	}

	public VarBinaryType(int length) {
		this(true, length);
	}

	public VarBinaryType() {
		this(DEFAULT_LENGTH);
	}

	/**
	 * Helper constructor for {@link #ofEmptyLiteral()} and {@link #copy(boolean)}.
	 */
	private VarBinaryType(int length, boolean isNullable) {
		super(isNullable, LogicalTypeRoot.VARBINARY);
		this.length = length;
	}

	/**
	 * The SQL standard defines that character string literals are allowed to be zero-length strings
	 * (i.e., to contain no characters) even though it is not permitted to declare a type that is zero.
	 * For consistent behavior, the same logic applies to binary strings. This has also implications
	 * on variable-length binary strings during type inference because any fixed-length binary string
	 * should be convertible to a variable-length one.
	 *
	 * <p>This method enables this special kind of binary string.
	 *
	 * <p>Zero-length binary strings have no serializable string representation.
	 */
	public static VarBinaryType ofEmptyLiteral() {
		return new VarBinaryType(EMPTY_LITERAL_LENGTH, false);
	}

	public int getLength() {
		return length;
	}

	@Override
	public LogicalType copy(boolean isNullable) {
		return new VarBinaryType(length, isNullable);
	}

	@Override
	public String asSerializableString() {
		if (length == EMPTY_LITERAL_LENGTH) {
			throw new TableException(
				"Zero-length binary strings have no serializable string representation.");
		}
		return withNullability(FORMAT, length);
	}

	@Override
	public String asSummaryString() {
		if (length == MAX_LENGTH) {
			return withNullability(MAX_FORMAT);
		}
		return withNullability(FORMAT, length);
	}

	@Override
	public boolean supportsInputConversion(Class<?> clazz) {
		return INPUT_OUTPUT_CONVERSION.contains(clazz.getName());
	}

	@Override
	public boolean supportsOutputConversion(Class<?> clazz) {
		return INPUT_OUTPUT_CONVERSION.contains(clazz.getName());
	}

	@Override
	public Class<?> getDefaultConversion() {
		return DEFAULT_CONVERSION;
	}

	@Override
	public List<LogicalType> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public <R> R accept(LogicalTypeVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		VarBinaryType that = (VarBinaryType) o;
		return length == that.length;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), length);
	}
}
