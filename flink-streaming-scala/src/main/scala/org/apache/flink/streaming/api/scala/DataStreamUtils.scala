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

package org.apache.flink.streaming.api.scala

import org.apache.flink.annotation.Experimental
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.datastream.{DataStreamUtils => JavaStreamUtils}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
  * This class provides simple utility methods for collecting a [[DataStream]],
  * effectively enriching it with the functionality encapsulated by [[DataStreamUtils]].
  *
  * This experimental class is relocated from flink-streaming-contrib.
  *
  * @param self DataStream
  */
@Experimental
class DataStreamUtils[T: TypeInformation : ClassTag](val self: DataStream[T]) {

  /**
    * Returns a scala iterator to iterate over the elements of the DataStream.
    * @return The iterator
    */
  def collect() : Iterator[T] = {
    JavaStreamUtils.collect(self.javaStream).asScala
  }

  /**
    * Reinterprets the given [[DataStream]] as a [[KeyedStream]], which extracts keys with the
    * given [[KeySelectorWithType]].
    *
    * IMPORTANT: For every partition of the base stream, the keys of events in the base stream
    * must be partitioned exactly in the same way as if it was created through a
    * [[DataStream#keyBy(KeySelectorWithType)]].
    *
    * @param keySelector Function that defines how keys are extracted from the data stream.
    * @return The reinterpretation of the [[DataStream]] as a [[KeyedStream]].
    */
  def reinterpretAsKeyedStream[K: TypeInformation](
        keySelector: T => K): KeyedStream[T, K] = {

    val keySelectorWithType =
      new KeySelectorWithType[T, K](clean(keySelector), implicitly[TypeInformation[K]])

    asScalaStream(
      JavaStreamUtils.reinterpretAsKeyedStream(self.javaStream, keySelectorWithType))
  }

  private[flink] def clean[F <: AnyRef](f: F): F = {
    new StreamExecutionEnvironment(self.javaStream.getExecutionEnvironment).scalaClean(f)
  }
}

