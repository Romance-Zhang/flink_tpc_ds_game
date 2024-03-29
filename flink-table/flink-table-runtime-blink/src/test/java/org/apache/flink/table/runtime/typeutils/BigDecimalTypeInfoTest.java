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

package org.apache.flink.table.runtime.typeutils;

import org.apache.flink.api.common.typeutils.TypeInformationTestBase;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link BigDecimalTypeInfo}.
 */
public class BigDecimalTypeInfoTest extends TypeInformationTestBase<BigDecimalTypeInfo> {

	@Override
	protected BigDecimalTypeInfo[] getTestData() {
		return new BigDecimalTypeInfo[] {
				new BigDecimalTypeInfo(38, 18),
				new BigDecimalTypeInfo(17, 0),
				new BigDecimalTypeInfo(25, 21)
		};
	}

	@Test
	public void testEquality() {

		BigDecimalTypeInfo ta = new BigDecimalTypeInfo(10, 1);
		BigDecimalTypeInfo tb = new BigDecimalTypeInfo(10, 1);
		BigDecimalTypeInfo tx = new BigDecimalTypeInfo(10, 5);

		Assert.assertEquals(ta, tb);
		Assert.assertEquals(ta.hashCode(), tb.hashCode());
		Assert.assertNotEquals(ta, tx);
	}
}
