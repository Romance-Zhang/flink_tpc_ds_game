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

package org.apache.flink.configuration;

import org.apache.flink.annotation.PublicEvolving;

/**
 * Options which control the cluster behaviour.
 */
@PublicEvolving
public class ClusterOptions {

	public static final ConfigOption<Long> INITIAL_REGISTRATION_TIMEOUT = ConfigOptions
		.key("cluster.registration.initial-timeout")
		.defaultValue(100L)
		.withDescription("Initial registration timeout between cluster components in milliseconds.");

	public static final ConfigOption<Long> MAX_REGISTRATION_TIMEOUT = ConfigOptions
		.key("cluster.registration.max-timeout")
		.defaultValue(30000L)
		.withDescription("Maximum registration timeout between cluster components in milliseconds.");

	public static final ConfigOption<Long> ERROR_REGISTRATION_DELAY = ConfigOptions
		.key("cluster.registration.error-delay")
		.defaultValue(10000L)
		.withDescription("The pause made after an registration attempt caused an exception (other than timeout) in milliseconds.");

	public static final ConfigOption<Long> REFUSED_REGISTRATION_DELAY = ConfigOptions
		.key("cluster.registration.refused-registration-delay")
		.defaultValue(30000L)
		.withDescription("The pause made after the registration attempt was refused in milliseconds.");

	public static final ConfigOption<Long> CLUSTER_SERVICES_SHUTDOWN_TIMEOUT = ConfigOptions
		.key("cluster.services.shutdown-timeout")
		.defaultValue(30000L)
		.withDescription("The shutdown timeout for cluster services like executors in milliseconds.");
}
