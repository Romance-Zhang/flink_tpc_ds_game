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

package org.apache.flink.table.plan.logical

import java.util

import com.google.common.collect.ImmutableMap
import org.apache.calcite.rel.`type`.RelDataType
import org.apache.calcite.rel.{RelCollation, RelNode}
import org.apache.calcite.rex.RexNode

/**
  * Describes MATCH RECOGNIZE clause.
  */
case class MatchRecognize(
  input: RelNode,
  rowType: RelDataType,
  pattern: RexNode,
  patternDefinitions: ImmutableMap[String, RexNode],
  measures: ImmutableMap[String, RexNode],
  after: RexNode,
  subsets: ImmutableMap[String, util.SortedSet[String]],
  allRows: Boolean,
  partitionKeys: util.List[RexNode],
  orderKeys: RelCollation,
  interval: RexNode)
