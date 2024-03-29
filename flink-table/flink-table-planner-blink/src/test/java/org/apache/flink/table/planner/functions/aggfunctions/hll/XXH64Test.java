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

package org.apache.flink.table.planner.functions.aggfunctions.hll;

import org.apache.flink.core.memory.HeapMemorySegment;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Test the {@link XXH64} function.
 *
 * <p>Test constants were taken from the original implementation and the airlift/slice implementation.
 */
public class XXH64Test {

	private static final int SIZE = 101;
	private static final long DEFAULT_SEED = 0;
	private static final long PRIME = 2654435761L;
	private static final byte[] BUFFER = new byte[SIZE];
	private static final int TEST_INT = 0x4B1FFF9E; // First 4 bytes in the buffer
	private static final long TEST_LONG = 0xDD2F535E4B1FFF9EL; // First 8 bytes in the buffer

	/* Create the test data. */
	static {
		long seed = PRIME;
		for (int i = 0; i < SIZE; i++) {
			BUFFER[i] = (byte) (seed >> 24);
			seed *= seed;
		}
	}

	@Test
	public void testKnownIntegerInputs() {
		Assert.assertEquals(0x9256E58AA397AEF1L, XXH64.hashInt(TEST_INT, DEFAULT_SEED));
		Assert.assertEquals(0x9D5FFDFB928AB4BL, XXH64.hashInt(TEST_INT, PRIME));
	}

	@Test
	public void testKnownLongInputs() {
		Assert.assertEquals(0xF74CB1451B32B8CFL, XXH64.hashLong(TEST_LONG, DEFAULT_SEED));
		Assert.assertEquals(0x9C44B77FBCC302C5L, XXH64.hashLong(TEST_LONG, PRIME));
	}

	@Test
	public void testKnownByteArrayInputs() {
		HeapMemorySegment hms = HeapMemorySegment.FACTORY.wrap(BUFFER);
		int offset = 0;

		Assert.assertEquals(0xEF46DB3751D8E999L,
				XXH64.hashUnsafeBytes(hms, offset, 0, DEFAULT_SEED));
		Assert.assertEquals(0xAC75FDA2929B17EFL,
				XXH64.hashUnsafeBytes(hms, offset, 0, PRIME));
		Assert.assertEquals(0x4FCE394CC88952D8L,
				XXH64.hashUnsafeBytes(hms, offset, 1, DEFAULT_SEED));
		Assert.assertEquals(0x739840CB819FA723L,
				XXH64.hashUnsafeBytes(hms, offset, 1, PRIME));

		if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
			Assert.assertEquals(0x9256E58AA397AEF1L,
					XXH64.hashUnsafeBytes(hms, offset, 4, DEFAULT_SEED));
			Assert.assertEquals(0x9D5FFDFB928AB4BL,
					XXH64.hashUnsafeBytes(hms, offset, 4, PRIME));
			Assert.assertEquals(0xF74CB1451B32B8CFL,
					XXH64.hashUnsafeBytes(hms, offset, 8, DEFAULT_SEED));
			Assert.assertEquals(0x9C44B77FBCC302C5L,
					XXH64.hashUnsafeBytes(hms, offset, 8, PRIME));
			Assert.assertEquals(0xCFFA8DB881BC3A3DL,
					XXH64.hashUnsafeBytes(hms, offset, 14, DEFAULT_SEED));
			Assert.assertEquals(0x5B9611585EFCC9CBL,
					XXH64.hashUnsafeBytes(hms, offset, 14, PRIME));
			Assert.assertEquals(0x0EAB543384F878ADL,
					XXH64.hashUnsafeBytes(hms, offset, SIZE, DEFAULT_SEED));
			Assert.assertEquals(0xCAA65939306F1E21L,
					XXH64.hashUnsafeBytes(hms, offset, SIZE, PRIME));
		} else {
			Assert.assertEquals(0x7F875412350ADDDCL,
					XXH64.hashUnsafeBytes(hms, offset, 4, DEFAULT_SEED));
			Assert.assertEquals(0x564D279F524D8516L,
					XXH64.hashUnsafeBytes(hms, offset, 4, PRIME));
			Assert.assertEquals(0x7D9F07E27E0EB006L,
					XXH64.hashUnsafeBytes(hms, offset, 8, DEFAULT_SEED));
			Assert.assertEquals(0x893CEF564CB7858L,
					XXH64.hashUnsafeBytes(hms, offset, 8, PRIME));
			Assert.assertEquals(0xC6198C4C9CC49E17L,
					XXH64.hashUnsafeBytes(hms, offset, 14, DEFAULT_SEED));
			Assert.assertEquals(0x4E21BEF7164D4BBL,
					XXH64.hashUnsafeBytes(hms, offset, 14, PRIME));
			Assert.assertEquals(0xBCF5FAEDEE1F2B5AL,
					XXH64.hashUnsafeBytes(hms, offset, SIZE, DEFAULT_SEED));
			Assert.assertEquals(0x6F680C877A358FE5L,
					XXH64.hashUnsafeBytes(hms, offset, SIZE, PRIME));
		}
	}

	@Test
	public void randomizedStressTest() {
		int size = 65536;
		Random rand = new Random();

		// A set used to track collision rate.
		Set<Long> hashcodes = new HashSet<>();
		for (int i = 0; i < size; i++) {
			int vint = rand.nextInt();
			long lint = rand.nextLong();
			Assert.assertEquals(XXH64.hashInt(vint, DEFAULT_SEED), XXH64.hashInt(vint, DEFAULT_SEED));
			Assert.assertEquals(XXH64.hashLong(lint, DEFAULT_SEED), XXH64.hashLong(lint, DEFAULT_SEED));

			hashcodes.add(XXH64.hashLong(lint, DEFAULT_SEED));
		}

		// A very loose bound.
		Assert.assertTrue(hashcodes.size() > size * 0.95d);
	}

	@Test
	public void randomizedStressTestBytes() {
		int size = 65536;
		Random rand = new Random();

		// A set used to track collision rate.
		Set<Long> hashcodes = new HashSet<>();
		for (int i = 0; i < size; i++) {
			int byteArrSize = rand.nextInt(100) * 8;
			byte[] bytes = new byte[byteArrSize];
			rand.nextBytes(bytes);
			HeapMemorySegment hms = HeapMemorySegment.FACTORY.wrap(bytes);

			Assert.assertEquals(
					XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED),
					XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED));

			hashcodes.add(XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED));
		}

		// A very loose bound.
		Assert.assertTrue(hashcodes.size() > size * 0.95d);
	}

	@Test
	public void randomizedStressTestPaddedStrings() {
		int size = 64000;

		// A set used to track collision rate.
		Set<Long> hashcodes = new HashSet<>();
		for (int i = 0; i < size; i++) {
			int byteArrSize = 8;
			byte[] strBytes = String.valueOf(i).getBytes(StandardCharsets.UTF_8);
			byte[] paddedBytes = new byte[byteArrSize];
			System.arraycopy(strBytes, 0, paddedBytes, 0, strBytes.length);
			HeapMemorySegment hms = HeapMemorySegment.FACTORY.wrap(paddedBytes);

			Assert.assertEquals(
					XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED),
					XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED));

			hashcodes.add(XXH64.hashUnsafeBytes(hms, 0, byteArrSize, DEFAULT_SEED));
		}

		// A very loose bound.
		Assert.assertTrue(hashcodes.size() > size * 0.95d);
	}
}
