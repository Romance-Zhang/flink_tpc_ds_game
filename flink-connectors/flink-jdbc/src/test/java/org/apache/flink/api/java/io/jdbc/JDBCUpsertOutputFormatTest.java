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

package org.apache.flink.api.java.io.jdbc;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.types.Row;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.flink.api.java.io.jdbc.JDBCOutputFormatTest.toRow;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for the {@link JDBCUpsertOutputFormat}.
 */
public class JDBCUpsertOutputFormatTest extends JDBCTestBase {

	private JDBCUpsertOutputFormat format;
	private String[] fieldNames;
	private String[] keyFields;

	@Before
	public void setup() {
		fieldNames = new String[]{"id", "title", "author", "price", "qty"};
		keyFields = new String[]{"id"};
	}

	@Test
	public void testJDBCOutputFormat() throws Exception {
		format = JDBCUpsertOutputFormat.builder()
				.setOptions(JDBCOptions.builder()
						.setDBUrl(DB_URL)
						.setTableName(OUTPUT_TABLE)
						.build())
				.setFieldNames(fieldNames)
				.setKeyFields(keyFields)
				.build();
		RuntimeContext context = Mockito.mock(RuntimeContext.class);
		ExecutionConfig config = Mockito.mock(ExecutionConfig.class);
		doReturn(config).when(context).getExecutionConfig();
		doReturn(true).when(config).isObjectReuseEnabled();
		format.setRuntimeContext(context);
		format.open(0, 1);

		for (TestEntry entry : TEST_DATA) {
			format.writeRecord(Tuple2.of(true, toRow(entry)));
		}
		format.flush();
		check(Arrays.stream(TEST_DATA).map(JDBCOutputFormatTest::toRow).toArray(Row[]::new));

		// override
		for (TestEntry entry : TEST_DATA) {
			format.writeRecord(Tuple2.of(true, toRow(entry)));
		}
		format.flush();
		check(Arrays.stream(TEST_DATA).map(JDBCOutputFormatTest::toRow).toArray(Row[]::new));

		// delete
		for (int i = 0; i < TEST_DATA.length / 2; i++) {
			format.writeRecord(Tuple2.of(false, toRow(TEST_DATA[i])));
		}
		Row[] expected = new Row[TEST_DATA.length - TEST_DATA.length / 2];
		for (int i = TEST_DATA.length / 2; i < TEST_DATA.length; i++) {
			expected[i - TEST_DATA.length / 2] = toRow(TEST_DATA[i]);
		}
		format.flush();
		check(expected);
	}

	private void check(Row[] rows) throws SQLException {
		check(rows, DB_URL, OUTPUT_TABLE, fieldNames);
	}

	static void check(Row[] rows, String url, String table, String[] fields) throws SQLException {
		try (
				Connection dbConn = DriverManager.getConnection(url);
				PreparedStatement statement = dbConn.prepareStatement("select * from " + table);
				ResultSet resultSet = statement.executeQuery()
		) {
			List<String> results = new ArrayList<>();
			while (resultSet.next()) {
				Row row = new Row(fields.length);
				for (int i = 0; i < fields.length; i++) {
					row.setField(i, resultSet.getObject(fields[i]));
				}
				results.add(row.toString());
			}
			String[] sortedExpect = Arrays.stream(rows).map(Row::toString).toArray(String[]::new);
			String[] sortedResult = results.toArray(new String[0]);
			Arrays.sort(sortedExpect);
			Arrays.sort(sortedResult);
			assertArrayEquals(sortedExpect, sortedResult);
		}
	}

	@After
	public void clearOutputTable() throws Exception {
		if (format != null) {
			format.close();
		}
		format = null;
		Class.forName(DRIVER_CLASS);
		try (
			Connection conn = DriverManager.getConnection(DB_URL);
			Statement stat = conn.createStatement()) {
			stat.execute("DELETE FROM " + OUTPUT_TABLE);

			stat.close();
			conn.close();
		}
	}
}
