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

import java.io.IOException;

/**
 * Type serializer for {@code Character}.
 */
@Internal
public final class CharSerializer extends TypeSerializerSingleton<Character> {

	private static final long serialVersionUID = 1L;

	/** Sharable instance of the CharSerializer. */
	public static final CharSerializer INSTANCE = new CharSerializer();

	private static final Character ZERO = (char) 0;

	@Override
	public boolean isImmutableType() {
		return true;
	}

	@Override
	public Character createInstance() {
		return ZERO;
	}

	@Override
	public Character copy(Character from) {
		return from;
	}

	@Override
	public Character copy(Character from, Character reuse) {
		return from;
	}

	@Override
	public int getLength() {
		return 2;
	}

	@Override
	public void serialize(Character record, DataOutputView target) throws IOException {
		target.writeChar(record);
	}

	@Override
	public Character deserialize(DataInputView source) throws IOException {
		return source.readChar();
	}

	@Override
	public Character deserialize(Character reuse, DataInputView source) throws IOException {
		return deserialize(source);
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		target.writeChar(source.readChar());
	}

	@Override
	public TypeSerializerSnapshot<Character> snapshotConfiguration() {
		return new CharSerializerSnapshot();
	}

	// ------------------------------------------------------------------------

	/**
	 * Serializer configuration snapshot for compatibility and format evolution.
	 */
	@SuppressWarnings("WeakerAccess")
	public static final class CharSerializerSnapshot extends SimpleTypeSerializerSnapshot<Character> {

		public CharSerializerSnapshot() {
			super(() -> INSTANCE);
		}
	}
}
