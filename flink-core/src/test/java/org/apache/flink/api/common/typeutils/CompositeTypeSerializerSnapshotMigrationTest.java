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

package org.apache.flink.api.common.typeutils;

import org.apache.flink.api.common.typeutils.base.GenericArraySerializer;
import org.apache.flink.api.common.typeutils.base.GenericArraySerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.IntSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.typeutils.runtime.EitherSerializer;
import org.apache.flink.api.java.typeutils.runtime.JavaEitherSerializerSnapshot;
import org.apache.flink.testutils.migration.MigrationVersion;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

/**
 * Migration tests for Composite Types.
 */
@RunWith(Parameterized.class)
public class CompositeTypeSerializerSnapshotMigrationTest extends TypeSerializerSnapshotMigrationTestBase<Object> {

	public CompositeTypeSerializerSnapshotMigrationTest(TestSpecification<Object> testSpecification) {
		super(testSpecification);
	}

	@SuppressWarnings("unchecked")
	@Parameterized.Parameters(name = "Test Specification = {0}")
	public static Collection<TestSpecification<?>> testSpecifications() {

		final TestSpecifications testSpecifications = new TestSpecifications(MigrationVersion.v1_6, MigrationVersion.v1_7);

		testSpecifications.add(
			"either-serializer",
			EitherSerializer.class,
			JavaEitherSerializerSnapshot.class,
			() -> new EitherSerializer<>(StringSerializer.INSTANCE, IntSerializer.INSTANCE));
		testSpecifications.add(
			"generic-array-serializer",
			GenericArraySerializer.class,
			GenericArraySerializerSnapshot.class,
			() -> new GenericArraySerializer<>(String.class, StringSerializer.INSTANCE));

		return testSpecifications.get();
	}
}
