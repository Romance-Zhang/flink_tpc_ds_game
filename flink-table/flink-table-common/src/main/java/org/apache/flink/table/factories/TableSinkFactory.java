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

package org.apache.flink.table.factories;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.sinks.TableSink;

import java.util.Map;

/**
 * A factory to create configured table sink instances in a batch or stream environment based on
 * string-based properties. See also {@link TableFactory} for more information.
 *
 * @param <T> type of records that the factory produces
 */
@PublicEvolving
public interface TableSinkFactory<T> extends TableFactory {

	/**
	 * Creates and configures a {@link TableSink} using the given properties.
	 *
	 * @param properties normalized properties describing a table sink.
	 * @return the configured table sink.
	 */
	TableSink<T> createTableSink(Map<String, String> properties);

	/**
	 * Creates and configures a {@link TableSink} based on the given {@link CatalogTable} instance.
	 *
	 * @param tablePath path of the given {@link CatalogTable}
	 * @param table {@link CatalogTable} instance.
	 * @return the configured table sink.
	 */
	default TableSink<T> createTableSink(ObjectPath tablePath, CatalogTable table) {
		return createTableSink(table.toProperties());
	}

}
