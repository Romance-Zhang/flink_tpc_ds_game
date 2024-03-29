/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.sqlexec;

import org.apache.flink.sql.parser.ddl.SqlCreateTable;
import org.apache.flink.sql.parser.dml.RichSqlInsert;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.calcite.FlinkPlannerImpl;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogManager;
import org.apache.flink.table.catalog.CatalogManagerCalciteSchema;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CatalogTableImpl;
import org.apache.flink.table.catalog.FunctionCatalog;
import org.apache.flink.table.catalog.GenericInMemoryCatalog;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.expressions.ExpressionBridge;
import org.apache.flink.table.expressions.PlannerExpressionConverter;
import org.apache.flink.table.operations.CatalogSinkModifyOperation;
import org.apache.flink.table.operations.Operation;
import org.apache.flink.table.operations.ddl.CreateTableOperation;
import org.apache.flink.table.planner.PlanningConfigurationBuilder;
import org.apache.flink.table.types.DataType;

import org.apache.calcite.sql.SqlNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.apache.calcite.jdbc.CalciteSchemaBuilder.asRootSchema;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Test cases for {@link SqlToOperationConverter}. **/
public class SqlToOperationConverterTest {
	private final TableConfig tableConfig = new TableConfig();
	private final Catalog catalog = new GenericInMemoryCatalog("MockCatalog",
		"default");
	private final CatalogManager catalogManager =
		new CatalogManager("builtin", catalog);
	private final FunctionCatalog functionCatalog = new FunctionCatalog(catalogManager);
	private final PlanningConfigurationBuilder planningConfigurationBuilder =
		new PlanningConfigurationBuilder(tableConfig,
			functionCatalog,
			asRootSchema(new CatalogManagerCalciteSchema(catalogManager, false)),
			new ExpressionBridge<>(functionCatalog,
				PlannerExpressionConverter.INSTANCE()));

	@Before
	public void before() throws TableAlreadyExistException, DatabaseNotExistException {
		final ObjectPath path1 = new ObjectPath(catalogManager.getCurrentDatabase(), "t1");
		final ObjectPath path2 = new ObjectPath(catalogManager.getCurrentDatabase(), "t2");
		final TableSchema tableSchema = TableSchema.builder()
			.field("a", DataTypes.BIGINT())
			.field("b", DataTypes.VARCHAR(Integer.MAX_VALUE))
			.field("c", DataTypes.INT())
			.field("d", DataTypes.VARCHAR(Integer.MAX_VALUE))
			.build();
		Map<String, String> properties = new HashMap<>();
		properties.put("connector", "COLLECTION");
		final CatalogTable catalogTable =  new CatalogTableImpl(tableSchema, properties, "");
		catalog.createTable(path1, catalogTable, true);
		catalog.createTable(path2, catalogTable, true);
	}

	@After
	public void after() throws TableNotExistException {
		final ObjectPath path1 = new ObjectPath(catalogManager.getCurrentDatabase(), "t1");
		final ObjectPath path2 = new ObjectPath(catalogManager.getCurrentDatabase(), "t2");
		catalog.dropTable(path1, true);
		catalog.dropTable(path2, true);
	}

	@Test
	public void testCreateTable() {
		final String sql = "CREATE TABLE tbl1 (\n" +
			"  a bigint,\n" +
			"  b varchar, \n" +
			"  c int, \n" +
			"  d varchar" +
			")\n" +
			"  PARTITIONED BY (a, d)\n" +
			"  with (\n" +
			"    'connector' = 'kafka', \n" +
			"    'kafka.topic' = 'log.test'\n" +
			")\n";
		final FlinkPlannerImpl planner = getPlannerBySqlDialect(SqlDialect.DEFAULT);
		SqlNode node = planner.parse(sql);
		assert node instanceof SqlCreateTable;
		Operation operation = SqlToOperationConverter.convert(planner, node);
		assert operation instanceof CreateTableOperation;
		CreateTableOperation op = (CreateTableOperation) operation;
		CatalogTable catalogTable = op.getCatalogTable();
		assertEquals(Arrays.asList("a", "d"), catalogTable.getPartitionKeys());
		assertArrayEquals(catalogTable.getSchema().getFieldNames(),
			new String[] {"a", "b", "c", "d"});
		assertArrayEquals(catalogTable.getSchema().getFieldDataTypes(),
			new DataType[]{
				DataTypes.BIGINT(),
				DataTypes.VARCHAR(Integer.MAX_VALUE),
				DataTypes.INT(),
				DataTypes.VARCHAR(Integer.MAX_VALUE)});
	}

	@Test
	public void testCreateTableWithMinusInOptionKey() {
		final String sql = "create table source_table(\n" +
			"  a int,\n" +
			"  b bigint,\n" +
			"  c varchar\n" +
			") with (\n" +
			"  'a-b-c-d124' = 'ab',\n" +
			"  'a.b-c-d.e-f.g' = 'ada',\n" +
			"  'a.b-c-d.e-f1231.g' = 'ada',\n" +
			"  'a.b-c-d.*' = 'adad')\n";
		final FlinkPlannerImpl planner = getPlannerBySqlDialect(SqlDialect.DEFAULT);
		SqlNode node = planner.parse(sql);
		assert node instanceof SqlCreateTable;
		Operation operation = SqlToOperationConverter.convert(planner, node);
		assert operation instanceof CreateTableOperation;
		CreateTableOperation op = (CreateTableOperation) operation;
		CatalogTable catalogTable = op.getCatalogTable();
		Map<String, String> properties = catalogTable.toProperties()
			.entrySet().stream()
			.filter(e -> !e.getKey().contains("schema"))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, String> sortedProperties = new TreeMap<>(properties);
		final String expected = "{a-b-c-d124=ab, "
			+ "a.b-c-d.*=adad, "
			+ "a.b-c-d.e-f.g=ada, "
			+ "a.b-c-d.e-f1231.g=ada}";
		assertEquals(expected, sortedProperties.toString());
	}

	@Test(expected = SqlConversionException.class)
	public void testCreateTableWithPkUniqueKeys() {
		final String sql = "CREATE TABLE tbl1 (\n" +
			"  a bigint,\n" +
			"  b varchar, \n" +
			"  c int, \n" +
			"  d varchar, \n" +
			"  primary key(a), \n" +
			"  unique(a, b) \n" +
			")\n" +
			"  PARTITIONED BY (a, d)\n" +
			"  with (\n" +
			"    'connector' = 'kafka', \n" +
			"    'kafka.topic' = 'log.test'\n" +
			")\n";
		final FlinkPlannerImpl planner = getPlannerBySqlDialect(SqlDialect.DEFAULT);
		SqlNode node = planner.parse(sql);
		assert node instanceof SqlCreateTable;
		SqlToOperationConverter.convert(planner, node);
	}

	@Test
	public void testSqlInsertWithStaticPartition() {
		final String sql = "insert into t1 partition(a=1) select b, c, d from t2";
		FlinkPlannerImpl planner = getPlannerBySqlDialect(SqlDialect.HIVE);
		SqlNode node = planner.parse(sql);
		assert node instanceof RichSqlInsert;
		Operation operation = SqlToOperationConverter.convert(planner, node);
		assert operation instanceof CatalogSinkModifyOperation;
		CatalogSinkModifyOperation sinkModifyOperation = (CatalogSinkModifyOperation) operation;
		final Map<String, String> expectedStaticPartitions = new HashMap<>();
		expectedStaticPartitions.put("a", "1");
		assertEquals(expectedStaticPartitions, sinkModifyOperation.getStaticPartitions());
	}

	private FlinkPlannerImpl getPlannerBySqlDialect(SqlDialect sqlDialect) {
		tableConfig.setSqlDialect(sqlDialect);
		return planningConfigurationBuilder.createFlinkPlanner(catalogManager.getCurrentCatalog(),
			catalogManager.getCurrentDatabase());
	}
}
