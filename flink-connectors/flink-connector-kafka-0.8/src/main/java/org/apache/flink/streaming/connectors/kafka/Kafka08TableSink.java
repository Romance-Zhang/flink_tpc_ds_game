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

package org.apache.flink.streaming.connectors.kafka;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;

import java.util.Optional;
import java.util.Properties;

/**
 * Kafka 0.8 table sink for writing data into Kafka.
 */
@Internal
public class Kafka08TableSink extends KafkaTableSinkBase {

	public Kafka08TableSink(
			TableSchema schema,
			String topic,
			Properties properties,
			Optional<FlinkKafkaPartitioner<Row>> partitioner,
			SerializationSchema<Row> serializationSchema) {
		super(
			schema,
			topic,
			properties,
			partitioner,
			serializationSchema);
	}

	@Override
	protected FlinkKafkaProducerBase<Row> createKafkaProducer(
			String topic,
			Properties properties,
			SerializationSchema<Row> serializationSchema,
			Optional<FlinkKafkaPartitioner<Row>> partitioner) {
		return new FlinkKafkaProducer08<>(
			topic,
			serializationSchema,
			properties,
			partitioner.orElse(null));
	}
}
