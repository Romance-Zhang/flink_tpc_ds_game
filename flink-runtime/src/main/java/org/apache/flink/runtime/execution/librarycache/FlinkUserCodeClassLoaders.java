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

package org.apache.flink.runtime.execution.librarycache;

import org.apache.flink.util.ChildFirstClassLoader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Gives the URLClassLoader a nicer name for debugging purposes.
 */
public class FlinkUserCodeClassLoaders {

	public static URLClassLoader parentFirst(URL[] urls, ClassLoader parent) {
		return new ParentFirstClassLoader(urls, parent);
	}

	public static URLClassLoader childFirst(
		URL[] urls,
		ClassLoader parent,
		String[] alwaysParentFirstPatterns) {
		return new ChildFirstClassLoader(urls, parent, alwaysParentFirstPatterns);
	}

	public static URLClassLoader create(
		ResolveOrder resolveOrder, URL[] urls, ClassLoader parent, String[] alwaysParentFirstPatterns) {

		switch (resolveOrder) {
			case CHILD_FIRST:
				return childFirst(urls, parent, alwaysParentFirstPatterns);
			case PARENT_FIRST:
				return parentFirst(urls, parent);
			default:
				throw new IllegalArgumentException("Unknown class resolution order: " + resolveOrder);
		}
	}

	/**
	 * Class resolution order for Flink URL {@link ClassLoader}.
	 */
	public enum ResolveOrder {
		CHILD_FIRST, PARENT_FIRST;

		public static ResolveOrder fromString(String resolveOrder) {
			if (resolveOrder.equalsIgnoreCase("parent-first")) {
				return PARENT_FIRST;
			} else if (resolveOrder.equalsIgnoreCase("child-first")) {
				return CHILD_FIRST;
			} else {
				throw new IllegalArgumentException("Unknown resolve order: " + resolveOrder);
			}
		}
	}

	/**
	 * Regular URLClassLoader that first loads from the parent and only after that from the URLs.
	 */
	static class ParentFirstClassLoader extends URLClassLoader {

		ParentFirstClassLoader(URL[] urls) {
			this(urls, FlinkUserCodeClassLoaders.class.getClassLoader());
		}

		ParentFirstClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}
	}
}
