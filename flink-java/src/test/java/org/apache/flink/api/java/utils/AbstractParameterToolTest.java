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

package org.apache.flink.api.java.utils;

import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.InstantiationUtil;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for tests for {@link ParameterTool}.
 */
public abstract class AbstractParameterToolTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	protected void validate(ParameterTool parameter) {
		ClosureCleaner.ensureSerializable(parameter);
		internalValidate(parameter);

		// -------- test behaviour after serialization ------------
		try {
			byte[] b = InstantiationUtil.serializeObject(parameter);
			final ParameterTool copy = InstantiationUtil.deserializeObject(b, getClass().getClassLoader());
			internalValidate(copy);
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void internalValidate(ParameterTool parameter) {
		Assert.assertEquals("myInput", parameter.getRequired("input"));
		Assert.assertEquals("myDefaultValue", parameter.get("output", "myDefaultValue"));
		Assert.assertNull(parameter.get("whatever"));
		Assert.assertEquals(15L, parameter.getLong("expectedCount", -1L));
		Assert.assertTrue(parameter.getBoolean("thisIsUseful", true));
		Assert.assertEquals(42, parameter.getByte("myDefaultByte", (byte) 42));
		Assert.assertEquals(42, parameter.getShort("myDefaultShort", (short) 42));

		final Configuration config = parameter.getConfiguration();
		Assert.assertEquals(15L, config.getLong("expectedCount", -1L));

		final Properties props = parameter.getProperties();
		Assert.assertEquals("myInput", props.getProperty("input"));

		// -------- test the default file creation ------------
		try {
			final String pathToFile = tmp.newFile().getAbsolutePath();
			parameter.createPropertiesFile(pathToFile);
			final Properties defaultProps = new Properties();
			try (FileInputStream fis = new FileInputStream(pathToFile)) {
				defaultProps.load(fis);
			}

			Assert.assertEquals("myDefaultValue", defaultProps.get("output"));
			Assert.assertEquals("-1", defaultProps.get("expectedCount"));
			Assert.assertTrue(defaultProps.containsKey("input"));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
