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

package org.apache.flink.types;

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataInputViewStreamWrapper;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.core.memory.DataOutputViewStreamWrapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PrimitiveDataTypeTest {

	private PipedInputStream in;
	private PipedOutputStream out;

	private DataInputView mIn;
	private DataOutputView mOut;

	@Before
	public void setup() throws Exception {
		in = new PipedInputStream(1000);
		out = new PipedOutputStream(in);
		mIn = new DataInputViewStreamWrapper(in);
		mOut = new DataOutputViewStreamWrapper(out);
	}

	@Test
	public void testIntValue() {
		IntValue int0 = new IntValue(10);
		// test value retrieval
		Assert.assertEquals(10, int0.getValue());
		// test value comparison
		IntValue int1 = new IntValue(10);
		IntValue int2 = new IntValue(-10);
		IntValue int3 = new IntValue(20);
		Assert.assertEquals(int0.compareTo(int0), 0);
		Assert.assertEquals(int0.compareTo(int1), 0);
		Assert.assertEquals(int0.compareTo(int2), 1);
		Assert.assertEquals(int0.compareTo(int3), -1);
		// test stream output and retrieval
		try {
			int0.write(mOut);
			int2.write(mOut);
			int3.write(mOut);
			IntValue int1n = new IntValue();
			IntValue int2n = new IntValue();
			IntValue int3n = new IntValue();
			int1n.read(mIn);
			int2n.read(mIn);
			int3n.read(mIn);
			Assert.assertEquals(int0.compareTo(int1n), 0);
			Assert.assertEquals(int0.getValue(), int1n.getValue());
			Assert.assertEquals(int2.compareTo(int2n), 0);
			Assert.assertEquals(int2.getValue(), int2n.getValue());
			Assert.assertEquals(int3.compareTo(int3n), 0);
			Assert.assertEquals(int3.getValue(), int3n.getValue());
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testDoubleValue() {
		DoubleValue double0 = new DoubleValue(10.2);
		// test value retrieval
		Assert.assertEquals(10.2, double0.getValue(), 0.0001);
		// test value comparison
		DoubleValue double1 = new DoubleValue(10.2);
		DoubleValue double2 = new DoubleValue(-10.5);
		DoubleValue double3 = new DoubleValue(20.2);
		Assert.assertEquals(double0.compareTo(double0), 0);
		Assert.assertEquals(double0.compareTo(double1), 0);
		Assert.assertEquals(double0.compareTo(double2), 1);
		Assert.assertEquals(double0.compareTo(double3), -1);
		// test stream output and retrieval
		try {
			double0.write(mOut);
			double2.write(mOut);
			double3.write(mOut);
			DoubleValue double1n = new DoubleValue();
			DoubleValue double2n = new DoubleValue();
			DoubleValue double3n = new DoubleValue();
			double1n.read(mIn);
			double2n.read(mIn);
			double3n.read(mIn);
			Assert.assertEquals(double0.compareTo(double1n), 0);
			Assert.assertEquals(double0.getValue(), double1n.getValue(), 0.0001);
			Assert.assertEquals(double2.compareTo(double2n), 0);
			Assert.assertEquals(double2.getValue(), double2n.getValue(), 0.0001);
			Assert.assertEquals(double3.compareTo(double3n), 0);
			Assert.assertEquals(double3.getValue(), double3n.getValue(), 0.0001);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}

	@Test
	public void testStringValue() {
		StringValue string0 = new StringValue("This is a test");
		StringValue stringThis = new StringValue("This");
		StringValue stringIsA = new StringValue("is a");
		// test value retrieval
		Assert.assertEquals("This is a test", string0.toString());
		// test value comparison
		StringValue string1 = new StringValue("This is a test");
		StringValue string2 = new StringValue("This is a tesa");
		StringValue string3 = new StringValue("This is a tesz");
		StringValue string4 = new StringValue("Ünlaut ßtring µ avec é y ¢");
		CharSequence chars5 = string1.subSequence(0, 4);
		StringValue string5 = (StringValue) chars5;
		StringValue string6 = (StringValue) string0.subSequence(0, string0.length());
		StringValue string7 = (StringValue) string0.subSequence(5, 9);
		StringValue string8 = (StringValue) string0.subSequence(0, 0);
		Assert.assertTrue(string0.compareTo(string0) == 0);
		Assert.assertTrue(string0.compareTo(string1) == 0);
		Assert.assertTrue(string0.compareTo(string2) > 0);
		Assert.assertTrue(string0.compareTo(string3) < 0);
		Assert.assertTrue(stringThis.equals(chars5));
		Assert.assertTrue(stringThis.compareTo(string5) == 0);
		Assert.assertTrue(string0.compareTo(string6) == 0);
		Assert.assertTrue(stringIsA.compareTo(string7) == 0);
		string7.setValue("This is a test");
		Assert.assertTrue(stringIsA.compareTo(string7) > 0);
		Assert.assertTrue(string0.compareTo(string7) == 0);
		string7.setValue("is a");
		Assert.assertTrue(stringIsA.compareTo(string7) == 0);
		Assert.assertTrue(string0.compareTo(string7) < 0);
		Assert.assertEquals(stringIsA.hashCode(), string7.hashCode());
		Assert.assertEquals(string7.length(), 4);
		Assert.assertEquals("is a", string7.getValue());
		Assert.assertEquals(string8.length(), 0);
		Assert.assertEquals("", string8.getValue());
		Assert.assertEquals('s', string7.charAt(1));
		try {
			string7.charAt(5);
			Assert.fail("Exception should have been thrown when accessing characters out of bounds.");
		} catch (IndexOutOfBoundsException iOOBE) {
			// expected
		}
		
		// test stream out/input
		try {
			string0.write(mOut);
			string4.write(mOut);
			string2.write(mOut);
			string3.write(mOut);
			string7.write(mOut);
			StringValue string1n = new StringValue();
			StringValue string2n = new StringValue();
			StringValue string3n = new StringValue();
			StringValue string4n = new StringValue();
			StringValue string7n = new StringValue();
			string1n.read(mIn);
			string4n.read(mIn);
			string2n.read(mIn);
			string3n.read(mIn);
			string7n.read(mIn);
			Assert.assertEquals(string0.compareTo(string1n), 0);
			Assert.assertEquals(string0.toString(), string1n.toString());
			Assert.assertEquals(string4.compareTo(string4n), 0);
			Assert.assertEquals(string4.toString(), string4n.toString());
			Assert.assertEquals(string2.compareTo(string2n), 0);
			Assert.assertEquals(string2.toString(), string2n.toString());
			Assert.assertEquals(string3.compareTo(string3n), 0);
			Assert.assertEquals(string3.toString(), string3n.toString());
			Assert.assertEquals(string7.compareTo(string7n), 0);
			Assert.assertEquals(string7.toString(), string7n.toString());
			try {
				string7n.charAt(5);
				Assert.fail("Exception should have been thrown when accessing characters out of bounds.");
			}
			catch (IndexOutOfBoundsException iOOBE) {
				// expected
			}
			
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testPactNull() {
		
		final NullValue pn1 = new NullValue();
		final NullValue pn2 = new NullValue();
		
		Assert.assertEquals("PactNull not equal to other PactNulls.", pn1, pn2);
		Assert.assertEquals("PactNull not equal to other PactNulls.", pn2, pn1);
		
		Assert.assertFalse("PactNull equal to other null.", pn1.equals(null));
		
		// test serialization
		final NullValue pn = new NullValue();
		final int numWrites = 13;
		
		try {
			// write it multiple times
			for (int i = 0; i < numWrites; i++) {
				pn.write(mOut);
			}
			
			// read it multiple times
			for (int i = 0; i < numWrites; i++) {
				pn.read(mIn);
			}
			
			
			Assert.assertEquals("Reading PactNull does not consume the same data as was written.", 0, in.available());
		}
		catch (IOException ioex) {
			Assert.fail("An exception occurred in the testcase: " + ioex.getMessage());
		}
	}
}
