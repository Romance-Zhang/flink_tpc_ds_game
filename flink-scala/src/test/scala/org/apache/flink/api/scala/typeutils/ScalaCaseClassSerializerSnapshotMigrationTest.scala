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

package org.apache.flink.api.scala.typeutils

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshotMigrationTestBase.{
  TestSpecification,
  TestSpecifications
}
import org.apache.flink.api.common.typeutils.{
  TypeSerializer,
  TypeSerializerSnapshotMigrationTestBase
}
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.api.scala.types.CustomCaseClass
import org.apache.flink.testutils.migration.MigrationVersion

import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.util

/**
  * Migration tests for the [[ScalaCaseClassSerializer]].
  */
@RunWith(classOf[Parameterized])
class ScalaCaseClassSerializerSnapshotMigrationTest(
  spec: TestSpecification[CustomCaseClass]
) extends TypeSerializerSnapshotMigrationTestBase[CustomCaseClass](spec) {}

object ScalaCaseClassSerializerSnapshotMigrationTest {

  private val typeInfo = createTypeInformation[CustomCaseClass]

  private val supplier =
    new util.function.Supplier[TypeSerializer[CustomCaseClass]] {
      override def get(): TypeSerializer[CustomCaseClass] =
        typeInfo.createSerializer(new ExecutionConfig)
    }

  @Parameterized.Parameters(name = "Test Specification = {0}")
  def testSpecifications(): util.Collection[TestSpecification[_]] = {
    val spec =
      new TestSpecifications(MigrationVersion.v1_6, MigrationVersion.v1_7)

    spec.add(
      "scala-case-class-serializer",
      classOf[ScalaCaseClassSerializer[CustomCaseClass]],
      classOf[ScalaCaseClassSerializerSnapshot[CustomCaseClass]],
      supplier
    )

    spec.get()
  }
}
