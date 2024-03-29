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

package org.apache.flink.table.types.logical.utils;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.CharType;
import org.apache.flink.table.types.logical.DayTimeIntervalType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeFamily;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TimeType;
import org.apache.flink.table.types.logical.TimestampKind;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.table.types.logical.YearMonthIntervalType;
import org.apache.flink.table.types.logical.ZonedTimestampType;

import java.util.List;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Utilities for checking {@link LogicalType} and avoiding a lot of type casting and repetitive work.
 */
@Internal
public final class LogicalTypeChecks {

	private static final TimestampKindExtractor TIMESTAMP_KIND_EXTRACTOR = new TimestampKindExtractor();

	private static final LengthExtractor LENGTH_EXTRACTOR = new LengthExtractor();

	private static final PrecisionExtractor PRECISION_EXTRACTOR = new PrecisionExtractor();

	private static final ScaleExtractor SCALE_EXTRACTOR = new ScaleExtractor();

	private static final YearPrecisionExtractor YEAR_PRECISION_EXTRACTOR = new YearPrecisionExtractor();

	private static final DayPrecisionExtractor DAY_PRECISION_EXTRACTOR = new DayPrecisionExtractor();

	private static final FractionalPrecisionExtractor FRACTIONAL_PRECISION_EXTRACTOR = new FractionalPrecisionExtractor();

	private static final SingleFieldIntervalExtractor SINGLE_FIELD_INTERVAL_EXTRACTOR = new SingleFieldIntervalExtractor();

	public static boolean hasRoot(LogicalType logicalType, LogicalTypeRoot typeRoot) {
		return logicalType.getTypeRoot() == typeRoot;
	}

	public static boolean hasFamily(LogicalType logicalType, LogicalTypeFamily family) {
		return logicalType.getTypeRoot().getFamilies().contains(family);
	}

	public static boolean isTimeAttribute(LogicalType logicalType) {
		return logicalType.accept(TIMESTAMP_KIND_EXTRACTOR) != TimestampKind.REGULAR;
	}

	public static boolean isRowtimeAttribute(LogicalType logicalType) {
		return logicalType.accept(TIMESTAMP_KIND_EXTRACTOR) == TimestampKind.ROWTIME;
	}

	public static boolean isProctimeAttribute(LogicalType logicalType) {
		return logicalType.accept(TIMESTAMP_KIND_EXTRACTOR) == TimestampKind.PROCTIME;
	}

	public static int getLength(LogicalType logicalType) {
		return logicalType.accept(LENGTH_EXTRACTOR);
	}

	public static boolean hasLength(LogicalType logicalType, int length) {
		return getLength(logicalType) == length;
	}

	/**
	 * Returns the precision of all types that define a precision implicitly or explicitly.
	 */
	public static int getPrecision(LogicalType logicalType) {
		return logicalType.accept(PRECISION_EXTRACTOR);
	}

	/**
	 * Checks the precision of a type that defines a precision implicitly or explicitly.
	 */
	public static boolean hasPrecision(LogicalType logicalType, int precision) {
		return getPrecision(logicalType) == precision;
	}

	/**
	 * Returns the scale of all types that define a scale implicitly or explicitly.
	 */
	public static int getScale(LogicalType logicalType) {
		return logicalType.accept(SCALE_EXTRACTOR);
	}

	/**
	 * Checks the scale of all types that define a scale implicitly or explicitly.
	 */
	public static boolean hasScale(LogicalType logicalType, int scale) {
		return getScale(logicalType) == scale;
	}

	public static int getYearPrecision(LogicalType logicalType) {
		return logicalType.accept(YEAR_PRECISION_EXTRACTOR);
	}

	public static boolean hasYearPrecision(LogicalType logicalType, int yearPrecision) {
		return getYearPrecision(logicalType) == yearPrecision;
	}

	public static int getDayPrecision(LogicalType logicalType) {
		return logicalType.accept(DAY_PRECISION_EXTRACTOR);
	}

	public static boolean hasDayPrecision(LogicalType logicalType, int yearPrecision) {
		return getDayPrecision(logicalType) == yearPrecision;
	}

	public static int getFractionalPrecision(LogicalType logicalType) {
		return logicalType.accept(FRACTIONAL_PRECISION_EXTRACTOR);
	}

	public static boolean hasFractionalPrecision(LogicalType logicalType, int fractionalPrecision) {
		return getFractionalPrecision(logicalType) == fractionalPrecision;
	}

	public static boolean isSingleFieldInterval(LogicalType logicalType) {
		return logicalType.accept(SINGLE_FIELD_INTERVAL_EXTRACTOR);
	}

	/**
	 * Returns true if the two given types are compatible. Types are compatible is for atomic types
	 * (VARCHAR, INT, BOOLEAN, etc..), they must be fully equal (i.e. {@link LogicalType#equals(Object)}),
	 * for complex types (ARRAY, ROW, MAP, etc..), they must be in the same type but ignore field
	 * names and other logical attributes, and all the children types ({@link LogicalType#getChildren()})
	 * must be compatible too.
	 */
	public static boolean areTypesCompatible(LogicalType thisType, LogicalType thatType) {
		checkNotNull(thisType);
		checkNotNull(thatType);
		TypeCompatibleVisitor visitor = new TypeCompatibleVisitor(thisType);
		return thatType.accept(visitor);
	}

	private LogicalTypeChecks() {
		// no instantiation
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * Extracts an attribute of logical types that define that attribute.
	 */
	private static class Extractor<T> extends LogicalTypeDefaultVisitor<T> {
		@Override
		protected T defaultMethod(LogicalType logicalType) {
			throw new IllegalArgumentException(
				String.format(
					"Invalid use of extractor %s. Called on logical type: %s",
					this.getClass().getName(),
					logicalType));
		}
	}

	private static class LengthExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(CharType charType) {
			return charType.getLength();
		}

		@Override
		public Integer visit(VarCharType varCharType) {
			return varCharType.getLength();
		}

		@Override
		public Integer visit(BinaryType binaryType) {
			return binaryType.getLength();
		}

		@Override
		public Integer visit(VarBinaryType varBinaryType) {
			return varBinaryType.getLength();
		}
	}

	private static class PrecisionExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(DecimalType decimalType) {
			return decimalType.getPrecision();
		}

		@Override
		public Integer visit(TinyIntType tinyIntType) {
			return TinyIntType.PRECISION;
		}

		@Override
		public Integer visit(SmallIntType smallIntType) {
			return SmallIntType.PRECISION;
		}

		@Override
		public Integer visit(IntType intType) {
			return IntType.PRECISION;
		}

		@Override
		public Integer visit(BigIntType bigIntType) {
			return BigIntType.PRECISION;
		}

		@Override
		public Integer visit(TimeType timeType) {
			return timeType.getPrecision();
		}

		@Override
		public Integer visit(TimestampType timestampType) {
			return timestampType.getPrecision();
		}

		@Override
		public Integer visit(ZonedTimestampType zonedTimestampType) {
			return zonedTimestampType.getPrecision();
		}

		@Override
		public Integer visit(LocalZonedTimestampType localZonedTimestampType) {
			return localZonedTimestampType.getPrecision();
		}
	}

	private static class ScaleExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(DecimalType decimalType) {
			return decimalType.getScale();
		}

		@Override
		public Integer visit(TinyIntType tinyIntType) {
			return 0;
		}

		@Override
		public Integer visit(SmallIntType smallIntType) {
			return 0;
		}

		@Override
		public Integer visit(IntType intType) {
			return 0;
		}

		@Override
		public Integer visit(BigIntType bigIntType) {
			return 0;
		}
	}

	private static class YearPrecisionExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(YearMonthIntervalType yearMonthIntervalType) {
			return yearMonthIntervalType.getYearPrecision();
		}
	}

	private static class DayPrecisionExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(DayTimeIntervalType dayTimeIntervalType) {
			return dayTimeIntervalType.getDayPrecision();
		}
	}

	private static class FractionalPrecisionExtractor extends Extractor<Integer> {

		@Override
		public Integer visit(DayTimeIntervalType dayTimeIntervalType) {
			return dayTimeIntervalType.getFractionalPrecision();
		}
	}

	private static class TimestampKindExtractor extends Extractor<TimestampKind>  {

		@Override
		public TimestampKind visit(TimestampType timestampType) {
			return timestampType.getKind();
		}

		@Override
		public TimestampKind visit(ZonedTimestampType zonedTimestampType) {
			return zonedTimestampType.getKind();
		}

		@Override
		public TimestampKind visit(LocalZonedTimestampType localZonedTimestampType) {
			return localZonedTimestampType.getKind();
		}
	}

	private static class SingleFieldIntervalExtractor extends Extractor<Boolean> {

		@Override
		public Boolean visit(YearMonthIntervalType yearMonthIntervalType) {
			switch (yearMonthIntervalType.getResolution()) {
				case YEAR:
				case MONTH:
					return true;
				default:
					return false;
			}
		}

		@Override
		public Boolean visit(DayTimeIntervalType dayTimeIntervalType) {
			switch (dayTimeIntervalType.getResolution()) {
				case DAY:
				case HOUR:
				case MINUTE:
				case SECOND:
					return true;
				default:
					return false;
			}
		}
	}

	private static class TypeCompatibleVisitor extends LogicalTypeDefaultVisitor<Boolean> {

		private final LogicalType thisType;

		private TypeCompatibleVisitor(LogicalType thisType) {
			checkNotNull(thisType);
			this.thisType = thisType;
		}

		@Override
		protected Boolean defaultMethod(LogicalType thatType) {
			checkNotNull(thatType);
			if (thisType == thatType) {
				return true;
			}
			if (thisType.getClass() != thatType.getClass() ||
				thisType.isNullable() != thatType.isNullable() ||
				thisType.getTypeRoot() != thatType.getTypeRoot()) {
				return false;
			}

			List<LogicalType> thisChildren = thisType.getChildren();
			List<LogicalType> thatChildren = thatType.getChildren();
			if (thisChildren.size() != thatChildren.size()) {
				return false;
			}
			if (thisChildren.isEmpty()) {
				// if it is an atomic type, delegate to equals method.
				return thisType.equals(thatType);
			} else {
				// if it is composite type, only need to check children types
				for (int i = 0; i < thisChildren.size(); i++) {
					LogicalType thisChild = thisChildren.get(i);
					LogicalType thatChild = thatChildren.get(i);
					if (!areTypesCompatible(thisChild, thatChild)) {
						return false;
					}
				}
				return true;
			}
		}
	}
}
