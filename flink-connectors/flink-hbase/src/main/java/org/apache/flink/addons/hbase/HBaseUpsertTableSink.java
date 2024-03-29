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

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.table.sinks.UpsertStreamTableSink;
import org.apache.flink.table.utils.TableConnectorUtils;
import org.apache.flink.types.Row;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;

import java.util.Arrays;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * An upsert {@link UpsertStreamTableSink} for HBase.
 */
public class HBaseUpsertTableSink implements UpsertStreamTableSink<Row> {

	private final HBaseTableSchema hbaseTableSchema;
	private final TableSchema tableSchema;
	private final HBaseOptions hbaseOptions;
	private final HBaseWriteOptions writeOptions;

	public HBaseUpsertTableSink(
			HBaseTableSchema hbaseTableSchema,
			HBaseOptions hbaseOptions,
			HBaseWriteOptions writeOptions) {
		checkArgument(hbaseTableSchema.getRowKeyName().isPresent(), "HBaseUpsertTableSink requires rowkey is set.");
		this.hbaseTableSchema = hbaseTableSchema;
		this.tableSchema = hbaseTableSchema.convertsToTableSchema();
		this.hbaseOptions = hbaseOptions;
		this.writeOptions = writeOptions;
	}

	@Override
	public void setKeyFields(String[] keys) {
		// hbase always upsert on rowkey, ignore query keys.
		// Actually, we should verify the query key is the same with rowkey.
		// However, the query key extraction doesn't work well in some scenarios
		// (e.g. concat key fields will lose key information). So we skip key validation currently.
	}

	@Override
	public void setIsAppendOnly(Boolean isAppendOnly) {
		// hbase always upsert on rowkey, even works in append only mode.
	}

	@Override
	public TypeInformation<Row> getRecordType() {
		return tableSchema.toRowType();
	}

	@Override
	public TableSchema getTableSchema() {
		return tableSchema;
	}

	@Override
	public DataStreamSink<?> consumeDataStream(DataStream<Tuple2<Boolean, Row>> dataStream) {
		Configuration hbaseClientConf = HBaseConfiguration.create();
		hbaseClientConf.set(HConstants.ZOOKEEPER_QUORUM, hbaseOptions.getZkQuorum());
		hbaseOptions.getZkNodeParent().ifPresent(v -> hbaseClientConf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, v));
		HBaseUpsertSinkFunction sinkFunction = new HBaseUpsertSinkFunction(
			hbaseOptions.getTableName(),
			hbaseTableSchema,
			hbaseClientConf,
			writeOptions.getBufferFlushMaxSizeInBytes(),
			writeOptions.getBufferFlushMaxRows(),
			writeOptions.getBufferFlushIntervalMillis());
		return dataStream
			.addSink(sinkFunction)
			.setParallelism(dataStream.getParallelism())
			.name(TableConnectorUtils.generateRuntimeName(this.getClass(), tableSchema.getFieldNames()));
	}

	@Override
	public void emitDataStream(DataStream<Tuple2<Boolean, Row>> dataStream) {
		consumeDataStream(dataStream);
	}

	@Override
	public TableSink<Tuple2<Boolean, Row>> configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
		if (!Arrays.equals(getFieldNames(), fieldNames) || !Arrays.equals(getFieldTypes(), fieldTypes)) {
			throw new ValidationException("Reconfiguration with different fields is not allowed. " +
				"Expected: " + Arrays.toString(getFieldNames()) + " / " + Arrays.toString(getFieldTypes()) + ". " +
				"But was: " + Arrays.toString(fieldNames) + " / " + Arrays.toString(fieldTypes));
		}

		return new HBaseUpsertTableSink(hbaseTableSchema, hbaseOptions, writeOptions);
	}

	@VisibleForTesting
	HBaseTableSchema getHBaseTableSchema() {
		return hbaseTableSchema;
	}

	@VisibleForTesting
	HBaseOptions getHBaseOptions() {
		return hbaseOptions;
	}

	@VisibleForTesting
	HBaseWriteOptions getWriteOptions() {
		return writeOptions;
	}
}
