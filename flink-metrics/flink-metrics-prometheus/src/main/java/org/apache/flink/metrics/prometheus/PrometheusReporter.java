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

package org.apache.flink.metrics.prometheus;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.metrics.Metric;
import org.apache.flink.metrics.MetricConfig;
import org.apache.flink.metrics.reporter.MetricReporter;
import org.apache.flink.util.NetUtils;
import org.apache.flink.util.Preconditions;

import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.util.Iterator;

/**
 * {@link MetricReporter} that exports {@link Metric Metrics} via Prometheus.
 */
@PublicEvolving
public class PrometheusReporter extends AbstractPrometheusReporter {

	static final String ARG_PORT = "port";
	private static final String DEFAULT_PORT = "9249";

	private HTTPServer httpServer;
	private int port;

	@VisibleForTesting
	int getPort() {
		Preconditions.checkState(httpServer != null, "Server has not been initialized.");
		return port;
	}

	@Override
	public void open(MetricConfig config) {
		super.open(config);

		String portsConfig = config.getString(ARG_PORT, DEFAULT_PORT);
		Iterator<Integer> ports = NetUtils.getPortRangeFromString(portsConfig);

		while (ports.hasNext()) {
			int port = ports.next();
			try {
				// internally accesses CollectorRegistry.defaultRegistry
				httpServer = new HTTPServer(port);
				this.port = port;
				log.info("Started PrometheusReporter HTTP server on port {}.", port);
				break;
			} catch (IOException ioe) { //assume port conflict
				log.debug("Could not start PrometheusReporter HTTP server on port {}.", port, ioe);
			}
		}
		if (httpServer == null) {
			throw new RuntimeException("Could not start PrometheusReporter HTTP server on any configured port. Ports: " + portsConfig);
		}
	}

	@Override
	public void close() {
		if (httpServer != null) {
			httpServer.stop();
		}

		super.close();
	}

}
