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

package org.apache.flink.graph.scala.utils

import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.graph.Edge

@SerialVersionUID(1L)
class Tuple3ToEdgeMap[K, EV] extends MapFunction[(K, K, EV), Edge[K, EV]] {

  override def map(value: (K, K, EV)): Edge[K, EV] = {
    new Edge(value._1, value._2, value._3)
  }
}
