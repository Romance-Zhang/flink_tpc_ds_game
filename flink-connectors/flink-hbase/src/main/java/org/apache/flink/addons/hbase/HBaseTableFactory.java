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

package org.apache.flink.addons.hbase;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.HBaseValidator;
import org.apache.flink.table.factories.StreamTableSinkFactory;
import org.apache.flink.table.factories.StreamTableSourceFactory;
import org.apache.flink.table.sinks.StreamTableSink;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.types.Row;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_PROPERTY_VERSION;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_TYPE;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_VERSION;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_TABLE_NAME;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_TYPE_VALUE_HBASE;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_VERSION_VALUE_143;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_WRITE_BUFFER_FLUSH_INTERVAL;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_WRITE_BUFFER_FLUSH_MAX_ROWS;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_WRITE_BUFFER_FLUSH_MAX_SIZE;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_ZK_NODE_PARENT;
import static org.apache.flink.table.descriptors.HBaseValidator.CONNECTOR_ZK_QUORUM;
import static org.apache.flink.table.descriptors.Schema.SCHEMA;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_NAME;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_TYPE;

/**
 * Factory for creating configured instances of {@link HBaseTableSource} or sink.
 */
public class HBaseTableFactory implements StreamTableSourceFactory<Row>, StreamTableSinkFactory<Tuple2<Boolean, Row>> {

	@Override
	public StreamTableSource<Row> createStreamTableSource(Map<String, String> properties) {
		final DescriptorProperties descriptorProperties = getValidatedProperties(properties);
		// create default configuration from current runtime env (`hbase-site.xml` in classpath) first,
		Configuration hbaseClientConf = HBaseConfiguration.create();
		String hbaseZk = descriptorProperties.getString(CONNECTOR_ZK_QUORUM);
		hbaseClientConf.set(HConstants.ZOOKEEPER_QUORUM, hbaseZk);
		descriptorProperties
			.getOptionalString(CONNECTOR_ZK_NODE_PARENT)
			.ifPresent(v -> hbaseClientConf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, v));

		String hTableName = descriptorProperties.getString(CONNECTOR_TABLE_NAME);
		TableSchema tableSchema = descriptorProperties.getTableSchema(SCHEMA);
		HBaseTableSchema hbaseSchema = validateTableSchema(tableSchema);
		return new HBaseTableSource(hbaseClientConf, hTableName, hbaseSchema, null);
	}

	@Override
	public StreamTableSink<Tuple2<Boolean, Row>> createStreamTableSink(Map<String, String> properties) {
		final DescriptorProperties descriptorProperties = getValidatedProperties(properties);
		HBaseOptions.Builder hbaseOptionsBuilder = HBaseOptions.builder();
		hbaseOptionsBuilder.setZkQuorum(descriptorProperties.getString(CONNECTOR_ZK_QUORUM));
		hbaseOptionsBuilder.setTableName(descriptorProperties.getString(CONNECTOR_TABLE_NAME));
		descriptorProperties
			.getOptionalString(CONNECTOR_ZK_NODE_PARENT)
			.ifPresent(hbaseOptionsBuilder::setZkNodeParent);

		TableSchema tableSchema = descriptorProperties.getTableSchema(SCHEMA);
		HBaseTableSchema hbaseSchema = validateTableSchema(tableSchema);

		HBaseWriteOptions.Builder writeBuilder = HBaseWriteOptions.builder();
		descriptorProperties
			.getOptionalInt(CONNECTOR_WRITE_BUFFER_FLUSH_MAX_ROWS)
			.ifPresent(writeBuilder::setBufferFlushMaxRows);
		descriptorProperties
			.getOptionalMemorySize(CONNECTOR_WRITE_BUFFER_FLUSH_MAX_SIZE)
			.ifPresent(v -> writeBuilder.setBufferFlushMaxSizeInBytes(v.getBytes()));
		descriptorProperties
			.getOptionalDuration(CONNECTOR_WRITE_BUFFER_FLUSH_INTERVAL)
			.ifPresent(v -> writeBuilder.setBufferFlushIntervalMillis(v.toMillis()));

		return new HBaseUpsertTableSink(
			hbaseSchema,
			hbaseOptionsBuilder.build(),
			writeBuilder.build()
		);
	}

	private HBaseTableSchema validateTableSchema(TableSchema schema) {
		HBaseTableSchema hbaseSchema = new HBaseTableSchema();
		String[] fieldNames = schema.getFieldNames();
		TypeInformation[] fieldTypes = schema.getFieldTypes();
		for (int i = 0; i < fieldNames.length; i++) {
			String name = fieldNames[i];
			TypeInformation<?> type = fieldTypes[i];
			if (type instanceof RowTypeInfo) {
				RowTypeInfo familyType = (RowTypeInfo) type;
				String[] qualifierNames = familyType.getFieldNames();
				TypeInformation[] qualifierTypes = familyType.getFieldTypes();
				for (int j = 0; j < familyType.getArity(); j++) {
					hbaseSchema.addColumn(name, qualifierNames[j], qualifierTypes[j].getTypeClass());
				}
			} else {
				hbaseSchema.setRowKey(name, type.getTypeClass());
			}
		}
		return hbaseSchema;
	}

	private DescriptorProperties getValidatedProperties(Map<String, String> properties) {
		final DescriptorProperties descriptorProperties = new DescriptorProperties(true);
		descriptorProperties.putProperties(properties);
		new HBaseValidator().validate(descriptorProperties);
		return descriptorProperties;
	}

	@Override
	public Map<String, String> requiredContext() {
		Map<String, String> context = new HashMap<>();
		context.put(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE_HBASE); // hbase
		context.put(CONNECTOR_VERSION, hbaseVersion()); // version
		context.put(CONNECTOR_PROPERTY_VERSION, "1"); // backwards compatibility
		return context;
	}

	@Override
	public List<String> supportedProperties() {
		List<String> properties = new ArrayList<>();

		properties.add(CONNECTOR_TABLE_NAME);
		properties.add(CONNECTOR_ZK_QUORUM);
		properties.add(CONNECTOR_ZK_NODE_PARENT);
		properties.add(CONNECTOR_WRITE_BUFFER_FLUSH_MAX_SIZE);
		properties.add(CONNECTOR_WRITE_BUFFER_FLUSH_MAX_ROWS);
		properties.add(CONNECTOR_WRITE_BUFFER_FLUSH_INTERVAL);

		// schema
		properties.add(SCHEMA + ".#." + SCHEMA_TYPE);
		properties.add(SCHEMA + ".#." + SCHEMA_NAME);

		return properties;
	}

	private String hbaseVersion() {
		return CONNECTOR_VERSION_VALUE_143;
	}
}
