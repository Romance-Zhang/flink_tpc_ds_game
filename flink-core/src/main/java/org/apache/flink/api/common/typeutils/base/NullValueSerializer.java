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

package org.apache.flink.api.common.typeutils.base;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.NullValue;

import java.io.IOException;

@Internal
public final class NullValueSerializer extends TypeSerializerSingleton<NullValue> {

	private static final long serialVersionUID = 1L;

	public static final NullValueSerializer INSTANCE = new NullValueSerializer();

	@Override
	public boolean isImmutableType() {
		return false;
	}

	@Override
	public NullValue createInstance() {
		return NullValue.getInstance();
	}

	@Override
	public NullValue copy(NullValue from) {
		return NullValue.getInstance();
	}

	@Override
	public NullValue copy(NullValue from, NullValue reuse) {
		return NullValue.getInstance();
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public void serialize(NullValue record, DataOutputView target) throws IOException {
	}

	@Override
	public NullValue deserialize(DataInputView source) throws IOException {
		return NullValue.getInstance();
	}

	@Override
	public NullValue deserialize(NullValue reuse, DataInputView source) throws IOException {
		return NullValue.getInstance();
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
	}

	@Override
	public TypeSerializerSnapshot<NullValue> snapshotConfiguration() {
		return new NullValueSerializerSnapshot();
	}

	// ------------------------------------------------------------------------

	/**
	 * Serializer configuration snapshot for compatibility and format evolution.
	 */
	@SuppressWarnings("WeakerAccess")
	public static final class NullValueSerializerSnapshot extends SimpleTypeSerializerSnapshot<NullValue> {

		public NullValueSerializerSnapshot() {
			super(() -> INSTANCE);
		}
	}
}
