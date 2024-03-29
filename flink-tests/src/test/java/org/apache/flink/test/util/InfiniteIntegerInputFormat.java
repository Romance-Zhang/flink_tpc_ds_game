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

package org.apache.flink.test.util;

import org.apache.flink.api.common.io.GenericInputFormat;

import java.io.IOException;

/**
 * Generates an infinite series of integer elements with optional read delay.
 */
public class InfiniteIntegerInputFormat extends GenericInputFormat<Integer> {
	private static final long serialVersionUID = 1L;
	private static final int DELAY = 20;
	private final boolean delay;

	public InfiniteIntegerInputFormat(boolean delay) {
		this.delay = delay;
	}

	@Override
	public boolean reachedEnd() throws IOException {
		return false;
	}

	@Override
	public Integer nextRecord(Integer reuse) throws IOException {
		if (delay) {
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException iex) {
				// do nothing
			}
		}
		return 1;
	}
}
