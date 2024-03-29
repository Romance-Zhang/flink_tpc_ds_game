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

package org.apache.flink.table.descriptors;

import org.apache.flink.annotation.Internal;

/**
 * Validator for {@link Metadata}.
 */
@Internal
public class MetadataValidator implements DescriptorValidator {

	public static final String METADATA_PROPERTY_VERSION = "metadata.property-version";
	public static final String METADATA_COMMENT = "metadata.comment";
	public static final String METADATA_CREATION_TIME = "metadata.creation-time";
	public static final String METADATA_LAST_ACCESS_TIME = "metadata.last-access-time";

	@Override
	public void validate(DescriptorProperties properties) {
		properties.validateInt(METADATA_PROPERTY_VERSION, true, 0, Integer.MAX_VALUE);
		properties.validateString(METADATA_COMMENT, true);
		properties.validateLong(METADATA_CREATION_TIME, true);
		properties.validateLong(METADATA_LAST_ACCESS_TIME, true);
	}
}
