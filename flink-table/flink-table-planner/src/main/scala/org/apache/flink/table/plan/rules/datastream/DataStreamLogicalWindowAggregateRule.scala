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

package org.apache.flink.table.plan.rules.datastream

import java.math.{BigDecimal => JBigDecimal}

import org.apache.calcite.rel.`type`.RelDataType
import org.apache.calcite.rex._
import org.apache.calcite.sql.`type`.{SqlTypeFamily, SqlTypeName}
import org.apache.flink.table.api.{TableException, ValidationException}
import org.apache.flink.table.calcite.FlinkTypeFactory
import org.apache.flink.table.catalog.BasicOperatorTable
import org.apache.flink.table.expressions.{Literal, PlannerResolvedFieldReference, WindowReference}
import org.apache.flink.table.plan.logical.{LogicalWindow, SessionGroupWindow, SlidingGroupWindow, TumblingGroupWindow}
import org.apache.flink.table.plan.rules.common.LogicalWindowAggregateRule
import org.apache.flink.table.typeutils.TimeIntervalTypeInfo

class DataStreamLogicalWindowAggregateRule
  extends LogicalWindowAggregateRule("DataStreamLogicalWindowAggregateRule") {

  /** Returns a reference to the time attribute with a time indicator type */
  override private[table] def getInAggregateGroupExpression(
      rexBuilder: RexBuilder,
      windowExpression: RexCall): RexNode = {

    val timeAttribute = windowExpression.operands.get(0)
    timeAttribute match {

      case _ if FlinkTypeFactory.isTimeIndicatorType(timeAttribute.getType) =>
        timeAttribute

      case _ =>
        throw new TableException(
          s"Time attribute expected but ${timeAttribute.getType} encountered.")
    }
  }

  /** Returns a zero literal of a timestamp type */
  override private[table] def getOutAggregateGroupExpression(
      rexBuilder: RexBuilder,
      windowExpression: RexCall): RexNode = {

    rexBuilder.makeLiteral(
      0L,
      rexBuilder.getTypeFactory.createSqlType(SqlTypeName.TIMESTAMP),
      true)
  }

  override private[table] def translateWindowExpression(
      windowExpr: RexCall,
      rowType: RelDataType)
    : LogicalWindow = {

    def getOperandAsLong(call: RexCall, idx: Int): Long =
      call.getOperands.get(idx) match {
        case v: RexLiteral if v.getTypeName.getFamily == SqlTypeFamily.INTERVAL_DAY_TIME =>
          v.getValue.asInstanceOf[JBigDecimal].longValue()
        case _ => throw new TableException(
          "Only constant window intervals with millisecond resolution are supported.")
      }

    def getOperandAsTimeIndicator(call: RexCall, idx: Int): PlannerResolvedFieldReference =
      call.getOperands.get(idx) match {
        case v: RexInputRef if FlinkTypeFactory.isTimeIndicatorType(v.getType) =>
          PlannerResolvedFieldReference(
            rowType.getFieldList.get(v.getIndex).getName,
            FlinkTypeFactory.toTypeInfo(v.getType))
        case _ =>
          throw new ValidationException("Window can only be defined over a time attribute column.")
      }

    windowExpr.getOperator match {
      case BasicOperatorTable.TUMBLE =>
        val time = getOperandAsTimeIndicator(windowExpr, 0)
        val interval = getOperandAsLong(windowExpr, 1)
        TumblingGroupWindow(
          WindowReference("w$", Some(time.resultType)),
          time,
          Literal(interval, TimeIntervalTypeInfo.INTERVAL_MILLIS)
        )

      case BasicOperatorTable.HOP =>
        val time = getOperandAsTimeIndicator(windowExpr, 0)
        val (slide, size) = (getOperandAsLong(windowExpr, 1), getOperandAsLong(windowExpr, 2))
        SlidingGroupWindow(
          WindowReference("w$", Some(time.resultType)),
          time,
          Literal(size, TimeIntervalTypeInfo.INTERVAL_MILLIS),
          Literal(slide, TimeIntervalTypeInfo.INTERVAL_MILLIS)
        )

      case BasicOperatorTable.SESSION =>
        val time = getOperandAsTimeIndicator(windowExpr, 0)
        val gap = getOperandAsLong(windowExpr, 1)
        SessionGroupWindow(
          WindowReference("w$", Some(time.resultType)),
          time,
          Literal(gap, TimeIntervalTypeInfo.INTERVAL_MILLIS)
        )
    }
  }
}

object DataStreamLogicalWindowAggregateRule {
  val INSTANCE = new DataStreamLogicalWindowAggregateRule
}
