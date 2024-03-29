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

package org.apache.flink.table.runtime.harness

import java.lang.{Long => JLong}
import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.flink.api.common.time.Time
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.streaming.api.operators.KeyedProcessOperator
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord
import org.apache.flink.table.api.{StreamQueryConfig, Types}
import org.apache.flink.table.runtime.aggregate._
import org.apache.flink.table.runtime.harness.HarnessTestBase._
import org.apache.flink.table.runtime.types.CRow
import org.apache.flink.types.Row
import org.junit.Test

class OverWindowHarnessTest extends HarnessTestBase{

  protected var queryConfig: StreamQueryConfig =
    new TestStreamQueryConfig(Time.seconds(2), Time.seconds(3))

  @Test
  def testProcTimeBoundedRowsOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new ProcTimeBoundedRowsOver[String](
        genMinMaxAggFunction,
        2,
        minMaxAggregationStateType,
        minMaxCRowType,
        queryConfig))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        Types.STRING)

    testHarness.open()

    // register cleanup timer with 3001
    testHarness.setProcessingTime(1)

    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 1L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "bbb", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 2L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 3L: JLong)))

    // register cleanup timer with 4100
    testHarness.setProcessingTime(1100)
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "bbb", 20L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 4L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(1L: JLong, "bbb", 30L: JLong)))

    // register cleanup timer with 6001
    testHarness.setProcessingTime(3001)
    testHarness.processElement(new StreamRecord(
      CRow(2L: JLong, "aaa", 7L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(2L: JLong, "aaa", 8L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(2L: JLong, "aaa", 9L: JLong)))

    // trigger cleanup timer and register cleanup timer with 9002
    testHarness.setProcessingTime(6002)
    testHarness.processElement(new StreamRecord(
        CRow(2L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(2L: JLong, "bbb", 40L: JLong)))

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 3L: JLong, 2L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "bbb", 20L: JLong, 10L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 4L: JLong, 3L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 5L: JLong, 4L: JLong, 5L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "aaa", 6L: JLong, 5L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(1L: JLong, "bbb", 30L: JLong, 20L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "aaa", 7L: JLong, 6L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "aaa", 8L: JLong, 7L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "aaa", 9L: JLong, 8L: JLong, 9L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "aaa", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "bbb", 40L: JLong, 40L: JLong, 40L: JLong)))

    verify(expectedOutput, result)

    testHarness.close()
  }

  /**
    * NOTE: all elements at the same proc timestamp have the same value per key
    */
  @Test
  def testProcTimeBoundedRangeOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new ProcTimeBoundedRangeOver[String](
        genMinMaxAggFunction,
        4000,
        minMaxAggregationStateType,
        minMaxCRowType,
        queryConfig))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        Types.STRING)

    testHarness.open()

    // register cleanup timer with 3003
    testHarness.setProcessingTime(3)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 1L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 10L: JLong)))

    testHarness.setProcessingTime(4)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 2L: JLong)))

    // trigger cleanup timer and register cleanup timer with 6003
    testHarness.setProcessingTime(3003)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 3L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 20L: JLong)))

    testHarness.setProcessingTime(5)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 4L: JLong)))

    // register cleanup timer with 9002
    testHarness.setProcessingTime(6002)

    testHarness.setProcessingTime(7002)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 30L: JLong)))

    // register cleanup timer with 14002
    testHarness.setProcessingTime(11002)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 7L: JLong)))

    testHarness.setProcessingTime(11004)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 8L: JLong)))

    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 40L: JLong)))

    testHarness.setProcessingTime(11006)

    // test for clean-up timer NPE
    testHarness.setProcessingTime(20000)

    // timer registered for 23000
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "ccc", 10L: JLong)))

    // update clean-up timer to 25500. Previous timer should not clean up
    testHarness.setProcessingTime(22500)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "ccc", 20L: JLong)))

    // 23000 clean-up timer should fire but not fail with an NPE
    testHarness.setProcessingTime(23001)

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    // all elements at the same proc timestamp have the same value per key
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 3L: JLong, 3L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 20L: JLong, 20L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 4L: JLong, 4L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 5L: JLong, 5L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 6L: JLong, 5L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 30L: JLong, 30L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 7L: JLong, 7L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 8L: JLong, 7L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 9L: JLong, 7L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 10L: JLong, 7L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 40L: JLong, 40L: JLong, 40L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "ccc", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "ccc", 20L: JLong, 10L: JLong, 20L: JLong)))

    verify(expectedOutput, result)

    testHarness.close()
  }

  @Test
  def testProcTimeUnboundedOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new ProcTimeUnboundedOver[String](
        genMinMaxAggFunction,
        minMaxAggregationStateType,
        queryConfig))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        Types.STRING)

    testHarness.open()

    // register cleanup timer with 4003
    testHarness.setProcessingTime(1003)

    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 1L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 2L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 3L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 20L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 4L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 30L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 7L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 8L: JLong)))

    // trigger cleanup timer and register cleanup timer with 8003
    testHarness.setProcessingTime(5003)
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(0L: JLong, "bbb", 40L: JLong)))

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 3L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 20L: JLong, 10L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 4L: JLong, 1L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 5L: JLong, 1L: JLong, 5L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 6L: JLong, 1L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 30L: JLong, 10L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 7L: JLong, 1L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 8L: JLong, 1L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 9L: JLong, 9L: JLong, 9L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "aaa", 10L: JLong, 9L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(0L: JLong, "bbb", 40L: JLong, 40L: JLong, 40L: JLong)))

    verify(expectedOutput, result)
    testHarness.close()
  }

  /**
    * all elements at the same row-time have the same value per key
    */
  @Test
  def testRowTimeBoundedRangeOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new RowTimeBoundedRangeOver[String](
        genMinMaxAggFunction,
        minMaxAggregationStateType,
        minMaxCRowType,
        4000,
        0,
        new TestStreamQueryConfig(Time.seconds(1), Time.seconds(2))))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        BasicTypeInfo.STRING_TYPE_INFO)

    testHarness.open()

    testHarness.processWatermark(1)
    testHarness.processElement(new StreamRecord(
      CRow(2L: JLong, "aaa", 1L: JLong)))

    testHarness.processWatermark(2)
    testHarness.processElement(new StreamRecord(
      CRow(3L: JLong, "bbb", 10L: JLong)))

    testHarness.processWatermark(4000)
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong)))

    testHarness.processWatermark(4001)
    testHarness.processElement(new StreamRecord(
      CRow(4002L: JLong, "aaa", 3L: JLong)))

    testHarness.processWatermark(4002)
    testHarness.processElement(new StreamRecord(
      CRow(4003L: JLong, "aaa", 4L: JLong)))

    testHarness.processWatermark(4800)
    testHarness.processElement(new StreamRecord(
      CRow(4801L: JLong, "bbb", 25L: JLong)))

    testHarness.processWatermark(6500)
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong)))

    testHarness.processWatermark(7000)
    testHarness.processElement(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong)))

    testHarness.processWatermark(8000)
    testHarness.processElement(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong)))

    testHarness.processWatermark(12000)
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong)))

    testHarness.processWatermark(19000)

    // test cleanup
    testHarness.setProcessingTime(1000)
    testHarness.processWatermark(20000)

    // check that state is removed after max retention time
    testHarness.processElement(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong))) // clean-up 3000
    testHarness.setProcessingTime(2500)
    testHarness.processElement(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong))) // clean-up 4500
    testHarness.processWatermark(20010) // compute output

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(4499)
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(4500)
    val x = testHarness.numKeyedStateEntries()
    assert(testHarness.numKeyedStateEntries() == 0) // check that all state is gone

    // check that state is only removed if all data was processed
    testHarness.processElement(new StreamRecord(
      CRow(20011L: JLong, "ccc", 3L: JLong))) // clean-up 6500

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(6500) // clean-up attempt but rescheduled to 8500
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state

    testHarness.processWatermark(20020) // schedule emission

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(8499) // clean-up
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(8500) // clean-up
    assert(testHarness.numKeyedStateEntries() == 0) // check that all state is gone

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    // all elements at the same row-time have the same value per key
    expectedOutput.add(new StreamRecord(
      CRow(2L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(3L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4002L: JLong, "aaa", 3L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4003L: JLong, "aaa", 4L: JLong, 2L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4801L: JLong, "bbb", 25L: JLong, 25L: JLong, 25L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong, 2L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong, 2L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong, 2L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong, 2L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong, 25L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong, 8L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong, 8L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong, 40L: JLong, 40L: JLong)))

    expectedOutput.add(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20011L: JLong, "ccc", 3L: JLong, 3L: JLong, 3L: JLong)))

    verify(expectedOutput, result)
    testHarness.close()
  }

  @Test
  def testRowTimeBoundedRowsOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new RowTimeBoundedRowsOver[String](
        genMinMaxAggFunction,
        minMaxAggregationStateType,
        minMaxCRowType,
        3,
        0,
        new TestStreamQueryConfig(Time.seconds(1), Time.seconds(2))))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        BasicTypeInfo.STRING_TYPE_INFO)

    testHarness.open()

    testHarness.processWatermark(800)
    testHarness.processElement(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong)))

    testHarness.processWatermark(2500)
    testHarness.processElement(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong)))

    testHarness.processWatermark(4000)
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong)))

    testHarness.processWatermark(4800)
    testHarness.processElement(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong)))

    testHarness.processWatermark(6500)
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong)))

    testHarness.processWatermark(7000)
    testHarness.processElement(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong)))

    testHarness.processWatermark(8000)
    testHarness.processElement(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong)))

    testHarness.processWatermark(12000)
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong)))

    testHarness.processWatermark(19000)

    // test cleanup
    testHarness.setProcessingTime(1000)
    testHarness.processWatermark(20000)

    // check that state is removed after max retention time
    testHarness.processElement(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong))) // clean-up 3000
    testHarness.setProcessingTime(2500)
    testHarness.processElement(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong))) // clean-up 4500
    testHarness.processWatermark(20010) // compute output

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(4499)
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(4500)
    assert(testHarness.numKeyedStateEntries() == 0) // check that all state is gone

    // check that state is only removed if all data was processed
    testHarness.processElement(new StreamRecord(
      CRow(20011L: JLong, "ccc", 3L: JLong))) // clean-up 6500

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(6500) // clean-up attempt but rescheduled to 8500
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state

    testHarness.processWatermark(20020) // schedule emission

    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(8499) // clean-up
    assert(testHarness.numKeyedStateEntries() > 0) // check that we have state
    testHarness.setProcessingTime(8500) // clean-up
    assert(testHarness.numKeyedStateEntries() == 0) // check that all state is gone


    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    expectedOutput.add(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong, 10L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong, 2L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong, 3L: JLong, 5L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong, 4L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong, 10L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong, 5L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong, 6L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong, 7L: JLong, 9L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong, 8L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong, 20L: JLong, 40L: JLong)))

    expectedOutput.add(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20011L: JLong, "ccc", 3L: JLong, 3L: JLong, 3L: JLong)))

    verify(expectedOutput, result)
    testHarness.close()
  }

  /**
    * all elements at the same row-time have the same value per key
    */
  @Test
  def testRowTimeUnboundedRangeOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new RowTimeUnboundedRangeOver[String](
        genMinMaxAggFunction,
        minMaxAggregationStateType,
        minMaxCRowType,
        0,
        new TestStreamQueryConfig(Time.seconds(1), Time.seconds(2))))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        BasicTypeInfo.STRING_TYPE_INFO)

    testHarness.open()

    testHarness.setProcessingTime(1000)
    testHarness.processWatermark(800)
    testHarness.processElement(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong)))

    testHarness.processWatermark(2500)
    testHarness.processElement(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong)))

    testHarness.processWatermark(4000)
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong)))

    testHarness.processWatermark(4800)
    testHarness.processElement(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong)))

    testHarness.processWatermark(6500)
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong)))

    testHarness.processWatermark(7000)
    testHarness.processElement(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong)))

    testHarness.processWatermark(8000)
    testHarness.processElement(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong)))

    testHarness.processWatermark(12000)
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong)))

    testHarness.processWatermark(19000)

    // test cleanup
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(2999) // clean up timer is 3000, so nothing should happen
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(3000) // clean up is triggered
    assert(testHarness.numKeyedStateEntries() == 0)

    testHarness.processWatermark(20000)
    testHarness.processElement(new StreamRecord(
      CRow(20000L: JLong, "ccc", 1L: JLong))) // test for late data

    testHarness.processElement(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong))) // clean-up 5000
    testHarness.setProcessingTime(2500)
    testHarness.processElement(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong))) // clean-up 5000

    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(5000) // does not clean up, because data left. New timer 7000
    testHarness.processWatermark(20010) // compute output

    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(6999) // clean up timer is 3000, so nothing should happen
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(7000) // clean up is triggered
    assert(testHarness.numKeyedStateEntries() == 0)

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    // all elements at the same row-time have the same value per key
    expectedOutput.add(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong, 10L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong, 1L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong, 1L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong, 1L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong, 10L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong, 1L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong, 1L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong, 1L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong, 1L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong, 10L: JLong, 40L: JLong)))

    expectedOutput.add(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong, 1L: JLong, 2L: JLong)))

    verify(expectedOutput, result)
    testHarness.close()
  }

  @Test
  def testRowTimeUnboundedRowsOver(): Unit = {

    val processFunction = new KeyedProcessOperator[String, CRow, CRow](
      new RowTimeUnboundedRowsOver[String](
        genMinMaxAggFunction,
        minMaxAggregationStateType,
        minMaxCRowType,
        0,
        new TestStreamQueryConfig(Time.seconds(1), Time.seconds(2))))

    val testHarness =
      createHarnessTester(
        processFunction,
        new TupleRowKeySelector[String](1),
        BasicTypeInfo.STRING_TYPE_INFO)

    testHarness.open()

    testHarness.setProcessingTime(1000)
    testHarness.processWatermark(800)
    testHarness.processElement(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong)))

    testHarness.processWatermark(2500)
    testHarness.processElement(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong)))

    testHarness.processWatermark(4000)
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong)))

    testHarness.processWatermark(4800)
    testHarness.processElement(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong)))

    testHarness.processWatermark(6500)
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong)))

    testHarness.processWatermark(7000)
    testHarness.processElement(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong)))

    testHarness.processWatermark(8000)
    testHarness.processElement(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong)))

    testHarness.processWatermark(12000)
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong)))
    testHarness.processElement(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong)))

    testHarness.processWatermark(19000)

    // test cleanup
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(2999) // clean up timer is 3000, so nothing should happen
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(3000) // clean up is triggered
    assert(testHarness.numKeyedStateEntries() == 0)

    testHarness.processWatermark(20000)
    testHarness.processElement(new StreamRecord(
      CRow(20000L: JLong, "ccc", 2L: JLong))) // test for late data

    testHarness.processElement(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong))) // clean-up 5000
    testHarness.setProcessingTime(2500)
    testHarness.processElement(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong))) // clean-up 5000

    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(5000) // does not clean up, because data left. New timer 7000
    testHarness.processWatermark(20010) // compute output

    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(6999) // clean up timer is 3000, so nothing should happen
    assert(testHarness.numKeyedStateEntries() > 0)
    testHarness.setProcessingTime(7000) // clean up is triggered
    assert(testHarness.numKeyedStateEntries() == 0)

    val result = testHarness.getOutput

    val expectedOutput = new ConcurrentLinkedQueue[Object]()

    expectedOutput.add(new StreamRecord(
      CRow(801L: JLong, "aaa", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(2501L: JLong, "bbb", 10L: JLong, 10L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 2L: JLong, 1L: JLong, 2L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "aaa", 3L: JLong, 1L: JLong, 3L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4001L: JLong, "bbb", 20L: JLong, 10L: JLong, 20L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(4801L: JLong, "aaa", 4L: JLong, 1L: JLong, 4L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 5L: JLong, 1L: JLong, 5L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "aaa", 6L: JLong, 1L: JLong, 6L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(6501L: JLong, "bbb", 30L: JLong, 10L: JLong, 30L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(7001L: JLong, "aaa", 7L: JLong, 1L: JLong, 7L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(8001L: JLong, "aaa", 8L: JLong, 1L: JLong, 8L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 9L: JLong, 1L: JLong, 9L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "aaa", 10L: JLong, 1L: JLong, 10L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(12001L: JLong, "bbb", 40L: JLong, 10L: JLong, 40L: JLong)))

    expectedOutput.add(new StreamRecord(
      CRow(20001L: JLong, "ccc", 1L: JLong, 1L: JLong, 1L: JLong)))
    expectedOutput.add(new StreamRecord(
      CRow(20002L: JLong, "ccc", 2L: JLong, 1L: JLong, 2L: JLong)))

    verify(expectedOutput, result)
    testHarness.close()
  }
}
