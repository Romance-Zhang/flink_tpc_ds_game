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

package org.apache.flink.api.scala.runtime

import java.io.FileOutputStream
import java.util.Collections

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.TypeSerializerSerializationUtil
import org.apache.flink.core.memory.DataOutputViewStreamWrapper

/**
  * Run this code on a 1.3 (or earlier) branch to generate the test data
  * for the [[TupleSerializerCompatibilityTest]].
  *
  * This generator is a separate file because a companion object would have side-effects on the
  * annotated classes generated by Scala.
  */
object TupleSerializerCompatibilityTestGenerator {

  case class TestCaseClass(
    i: Int,
    e: Either[String, Unit],
    t2: (Boolean, String),
    t1: (Double),
    t2ii: (Int, Int))

  val TEST_DATA_1 = TestCaseClass(42, Left("Hello"), (false, "what?"), 12.2, (12, 12))
  val TEST_DATA_2 = TestCaseClass(42, Right(), (false, "what?"), 12.2, (100, 200))
  val TEST_DATA_3 = TestCaseClass(100, Left("Hello"), (true, "what?"), 14.2, (-1, Int.MinValue))

  val SNAPSHOT_RESOURCE: String = "flink-1.3.2-scala-types-serializer-snapshot"

  val DATA_RESOURCE: String = "flink-1.3.2-scala-types-serializer-data"

  val SNAPSHOT_OUTPUT_PATH: String = "/tmp/snapshot/" + SNAPSHOT_RESOURCE

  val DATA_OUTPUT_PATH: String = "/tmp/snapshot/" + DATA_RESOURCE

  def main(args: Array[String]): Unit = {

    val typeInfo = org.apache.flink.api.scala.createTypeInformation[TestCaseClass]

    val serializer = typeInfo.createSerializer(new ExecutionConfig())
    val configSnapshot = serializer.snapshotConfiguration()

    var fos: FileOutputStream = null
    try {
      fos = new FileOutputStream(SNAPSHOT_OUTPUT_PATH)
      val out = new DataOutputViewStreamWrapper(fos)

      TypeSerializerSerializationUtil.writeSerializersAndConfigsWithResilience(
        out,
        Collections.singletonList(
          new org.apache.flink.api.java.tuple.Tuple2(serializer, configSnapshot)
        )
      )
    } finally {
      if (fos != null) {
        fos.close()
      }
    }

    fos = null
    try {
      fos = new FileOutputStream(DATA_OUTPUT_PATH)
      val out = new DataOutputViewStreamWrapper(fos)

      serializer.serialize(TEST_DATA_1, out)
      serializer.serialize(TEST_DATA_2, out)
      serializer.serialize(TEST_DATA_3, out)
    } finally {
      if (fos != null) {
        fos.close()
      }
    }
  }
}
