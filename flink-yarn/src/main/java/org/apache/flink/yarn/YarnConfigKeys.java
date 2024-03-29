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

package org.apache.flink.yarn;

/**
 * The Yarn environment variables used for settings of the containers.
 */
public class YarnConfigKeys {

	// ------------------------------------------------------------------------
	//  Environment variable names
	// ------------------------------------------------------------------------

	public static final String ENV_TM_MEMORY = "_CLIENT_TM_MEMORY";
	public static final String ENV_TM_COUNT = "_CLIENT_TM_COUNT";
	public static final String ENV_APP_ID = "_APP_ID";
	public static final String ENV_CLIENT_HOME_DIR = "_CLIENT_HOME_DIR";
	public static final String ENV_CLIENT_SHIP_FILES = "_CLIENT_SHIP_FILES";
	public static final String ENV_SLOTS = "_SLOTS";
	public static final String ENV_DETACHED = "_DETACHED";
	public static final String ENV_DYNAMIC_PROPERTIES = "_DYNAMIC_PROPERTIES";

	public static final String ENV_FLINK_CLASSPATH = "_FLINK_CLASSPATH";

	public static final String FLINK_JAR_PATH = "_FLINK_JAR_PATH"; // the Flink jar resource location (in HDFS).
	public static final String FLINK_YARN_FILES = "_FLINK_YARN_FILES"; // the root directory for all yarn application files

	public static final String KEYTAB_PATH = "_KEYTAB_PATH";
	public static final String KEYTAB_PRINCIPAL = "_KEYTAB_PRINCIPAL";
	public static final String ENV_HADOOP_USER_NAME = "HADOOP_USER_NAME";
	public static final String ENV_ZOOKEEPER_NAMESPACE = "_ZOOKEEPER_NAMESPACE";

	public static final String ENV_KRB5_PATH = "_KRB5_PATH";
	public static final String ENV_YARN_SITE_XML_PATH = "_YARN_SITE_XML_PATH";

	// ------------------------------------------------------------------------

	/** Private constructor to prevent instantiation. */
	private YarnConfigKeys() {}

}
