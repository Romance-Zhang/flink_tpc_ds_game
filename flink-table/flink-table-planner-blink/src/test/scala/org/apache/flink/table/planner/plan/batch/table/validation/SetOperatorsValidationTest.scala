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

package org.apache.flink.table.planner.plan.batch.table.validation

import org.apache.flink.api.scala._
import org.apache.flink.table.api.internal.TableEnvironmentImpl
import org.apache.flink.table.api.scala._
import org.apache.flink.table.api.{EnvironmentSettings, ValidationException}
import org.apache.flink.table.planner.runtime.utils.CollectionBatchExecTable
import org.apache.flink.table.planner.utils.TableTestBase

import org.junit._

class SetOperatorsValidationTest extends TableTestBase {

  @Test(expected = classOf[ValidationException])
  def testUnionDifferentColumnSize(): Unit = {
    val util = batchTestUtil()
    val ds1 = util.addTableSource[(Int, Long, String)]("Table3",'a, 'b, 'c)
    val ds2 = util.addTableSource[(Int, Long, Int, String, Long)]("Table5", 'a, 'b, 'd, 'c, 'e)

    // must fail. Union inputs have different column size.
    ds1.unionAll(ds2)
  }

  @Test(expected = classOf[ValidationException])
  def testUnionDifferentFieldTypes(): Unit = {
    val util = batchTestUtil()
    val ds1 = util.addTableSource[(Int, Long, String)]("Table3",'a, 'b, 'c)
    val ds2 = util.addTableSource[(Int, Long, Int, String, Long)]("Table5", 'a, 'b, 'c, 'd, 'e)
      .select('a, 'b, 'c)

    // must fail. Union inputs have different field types.
    ds1.unionAll(ds2)
  }

  @Test(expected = classOf[ValidationException])
  def testUnionTablesFromDifferentEnvs(): Unit = {
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tEnv1 = TableEnvironmentImpl.create(settings)
    val tEnv2 = TableEnvironmentImpl.create(settings)

    val ds1 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv1)
    val ds2 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv2)

    // Must fail. Tables are bound to different TableEnvironments.
    ds1.unionAll(ds2).select('c)
  }

  @Test(expected = classOf[ValidationException])
  def testMinusDifferentFieldTypes(): Unit = {
    val util = batchTestUtil()
    val ds1 = util.addTableSource[(Int, Long, String)]("Table3",'a, 'b, 'c)
    val ds2 = util.addTableSource[(Int, Long, Int, String, Long)]("Table5", 'a, 'b, 'c, 'd, 'e)
      .select('a, 'b, 'c)

    // must fail. Minus inputs have different field types.
    ds1.minus(ds2)
  }

  @Test(expected = classOf[ValidationException])
  def testMinusAllTablesFromDifferentEnvs(): Unit = {
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tEnv1 = TableEnvironmentImpl.create(settings)
    val tEnv2 = TableEnvironmentImpl.create(settings)

    val ds1 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv1)
    val ds2 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv2)

    // Must fail. Tables are bound to different TableEnvironments.
    ds1.minusAll(ds2).select('c)
  }

  @Test(expected = classOf[ValidationException])
  def testIntersectWithDifferentFieldTypes(): Unit = {
    val util = batchTestUtil()
    val ds1 = util.addTableSource[(Int, Long, String)]("Table3",'a, 'b, 'c)
    val ds2 = util.addTableSource[(Int, Long, Int, String, Long)]("Table5", 'a, 'b, 'c, 'd, 'e)
      .select('a, 'b, 'c)

    // must fail. Intersect inputs have different field types.
    ds1.intersect(ds2)
  }

  @Test(expected = classOf[ValidationException])
  def testIntersectTablesFromDifferentEnvs(): Unit = {
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tEnv1 = TableEnvironmentImpl.create(settings)
    val tEnv2 = TableEnvironmentImpl.create(settings)

    val ds1 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv1)
    val ds2 = CollectionBatchExecTable.getSmall3TupleDataSet(tEnv2)

    // Must fail. Tables are bound to different TableEnvironments.
    ds1.intersect(ds2).select('c)
  }
}
