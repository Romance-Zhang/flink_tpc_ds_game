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

package org.apache.flink.table.planner.plan.rules.physical.stream

import org.apache.flink.table.api.TableException
import org.apache.flink.table.planner.plan.`trait`.FlinkRelDistribution
import org.apache.flink.table.planner.plan.nodes.FlinkConventions
import org.apache.flink.table.planner.plan.nodes.logical.FlinkLogicalAggregate
import org.apache.flink.table.planner.plan.nodes.physical.stream.StreamExecGroupAggregate

import org.apache.calcite.plan.{RelOptRule, RelOptRuleCall}
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.convert.ConverterRule
import org.apache.calcite.rel.core.Aggregate.Group

import scala.collection.JavaConversions._

/**
  * Rule to convert a [[FlinkLogicalAggregate]] into a [[StreamExecGroupAggregate]].
  */
class StreamExecGroupAggregateRule
  extends ConverterRule(
    classOf[FlinkLogicalAggregate],
    FlinkConventions.LOGICAL,
    FlinkConventions.STREAM_PHYSICAL,
    "StreamExecGroupAggregateRule") {

  override def matches(call: RelOptRuleCall): Boolean = {
    val agg: FlinkLogicalAggregate = call.rel(0)

    // check if we have grouping sets
    if (agg.getGroupType != Group.SIMPLE || agg.indicator) {
      throw new TableException("GROUPING SETS are currently not supported.")
    }

    true
  }

  override def convert(rel: RelNode): RelNode = {
    val agg: FlinkLogicalAggregate = rel.asInstanceOf[FlinkLogicalAggregate]
    val requiredDistribution = if (agg.getGroupCount != 0) {
      FlinkRelDistribution.hash(agg.getGroupSet.asList)
    } else {
      FlinkRelDistribution.SINGLETON
    }
    val requiredTraitSet = rel.getCluster.getPlanner.emptyTraitSet()
      .replace(requiredDistribution)
      .replace(FlinkConventions.STREAM_PHYSICAL)
    val providedTraitSet = rel.getTraitSet.replace(FlinkConventions.STREAM_PHYSICAL)
    val newInput: RelNode = RelOptRule.convert(agg.getInput, requiredTraitSet)

    new StreamExecGroupAggregate(
      rel.getCluster,
      providedTraitSet,
      newInput,
      rel.getRowType,
      agg.getGroupSet.toArray,
      agg.getAggCallList,
      agg.partialFinalType)
  }
}

object StreamExecGroupAggregateRule {
  val INSTANCE: RelOptRule = new StreamExecGroupAggregateRule
}
