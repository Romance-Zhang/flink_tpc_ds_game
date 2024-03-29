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

package org.apache.flink.table.planner.runtime.utils

import org.apache.flink.api.common.typeinfo.BasicTypeInfo._
import org.apache.flink.api.common.typeinfo.LocalTimeTypeInfo.{LOCAL_DATE, LOCAL_DATE_TIME, LOCAL_TIME}
import org.apache.flink.api.java.tuple.{Tuple2 => JTuple2}
import org.apache.flink.api.java.typeutils.{RowTypeInfo, TupleTypeInfo}
import org.apache.flink.table.planner.runtime.utils.BatchTestBase.row
import org.apache.flink.table.planner.utils.DateTimeTestUtil._
import org.apache.flink.table.runtime.functions.SqlDateTimeUtils.unixTimestampToLocalDateTime
import org.apache.flink.types.Row

import java.math.{BigDecimal => JBigDecimal}

import scala.collection.{Seq, mutable}

object TestData {

  val type1 = new RowTypeInfo(INT_TYPE_INFO, STRING_TYPE_INFO, INT_TYPE_INFO)
  val type2 = new RowTypeInfo(
    INT_TYPE_INFO, LONG_TYPE_INFO, INT_TYPE_INFO, STRING_TYPE_INFO, LONG_TYPE_INFO)
  val type3 = new RowTypeInfo(INT_TYPE_INFO, LONG_TYPE_INFO, STRING_TYPE_INFO)
  val type4 = new RowTypeInfo(STRING_TYPE_INFO, INT_TYPE_INFO, INT_TYPE_INFO)
  val type5 = new RowTypeInfo(INT_TYPE_INFO, LONG_TYPE_INFO, INT_TYPE_INFO, STRING_TYPE_INFO,
    LONG_TYPE_INFO)
  val type6 = new RowTypeInfo(INT_TYPE_INFO, DOUBLE_TYPE_INFO, STRING_TYPE_INFO, LOCAL_DATE,
    LOCAL_TIME, LOCAL_DATE_TIME)

  val simpleType2 = new RowTypeInfo(INT_TYPE_INFO, DOUBLE_TYPE_INFO)

  val buildInType = new RowTypeInfo(BOOLEAN_TYPE_INFO, BYTE_TYPE_INFO, INT_TYPE_INFO,
    LONG_TYPE_INFO, DOUBLE_TYPE_INFO, STRING_TYPE_INFO, STRING_TYPE_INFO, LOCAL_DATE, LOCAL_TIME,
    LOCAL_DATE_TIME)
  val numericType = new RowTypeInfo(INT_TYPE_INFO, LONG_TYPE_INFO, FLOAT_TYPE_INFO,
    DOUBLE_TYPE_INFO, BIG_DEC_TYPE_INFO)

  val tupleIntInt = new TupleTypeInfo(INT_TYPE_INFO, INT_TYPE_INFO)
  val tupleStringInt = new TupleTypeInfo(STRING_TYPE_INFO, INT_TYPE_INFO)
  val genericType3 = new RowTypeInfo(tupleStringInt, tupleIntInt, INT_TYPE_INFO)
  val genericType5 = new RowTypeInfo(tupleIntInt, LONG_TYPE_INFO, INT_TYPE_INFO, STRING_TYPE_INFO,
    LONG_TYPE_INFO)
  val type3WithTimestamp = new RowTypeInfo(INT_TYPE_INFO, LONG_TYPE_INFO, STRING_TYPE_INFO,
    LOCAL_DATE_TIME)

  val nullablesOfData1 = Array(false, false, false)

  val nullableOfSimpleData2 = Array(false, false)

  val nullablesOfData2 = Array(false, false, false, false, false)

  val nullablesOfNullData2 = Array(true, false, false, false, false)

  lazy val data1: Seq[Row] = Seq(
    row(2, "a", 6),
    row(4, "b", 8),
    row(6, "c", 10),
    row(1, "a", 5),
    row(3, "b", 7),
    row(5, "c", 9)
  )

  lazy val data2 = Seq(
    row(2, 3L, 2, "Hallo Welt wie", 1L),
    row(3, 4L, 3, "Hallo Welt wie gehts?", 2L),
    row(2, 2L, 1, "Hallo Welt", 2L),
    row(1, 1L, 0, "Hallo", 1L),
    row(5, 11L, 10, "GHI", 1L),
    row(3, 5L, 4, "ABC", 2L),
    row(4, 10L, 9, "FGH", 2L),
    row(4, 7L, 6, "CDE", 2L),
    row(5, 14L, 13, "JKL", 2L),
    row(4, 9L, 8, "EFG", 1L),
    row(5, 15L, 14, "KLM", 2L),
    row(5, 12L, 11, "HIJ", 3L),
    row(4, 8L, 7, "DEF", 1L),
    row(5, 13L, 12, "IJK", 3L),
    row(3, 6L, 5, "BCD", 3L)
  )

  lazy val data4 = Seq(
    row("book", 1, 12),
    row("book", 2, 19),
    row("book", 4, 11),
    row("fruit", 4, 33),
    row("fruit", 3, 44),
    row("fruit", 5, 22)
  )

  lazy val nullData4: Seq[Row] = Seq(
    row("book", 1, 12),
    row("book", 2, null),
    row("book", 4, 11),
    row("fruit", 4, null),
    row("fruit", 3, 44),
    row("fruit", 5, null)
  )

  lazy val nullData2: Seq[Row] = data2 ++ Seq(
    row(null, 3L, 3, "NullTuple", 3L),
    row(null, 3L, 3, "NullTuple", 3L)
  )

  lazy val nullData3: Seq[Row] = data3 ++ Seq(
    row(null, 999L, "NullTuple"),
    row(null, 999L, "NullTuple")
  )

  lazy val allNullData3: Seq[Row] = Seq(
    row(null, null, null),
    row(null, null, null)
  )

  val allNullablesOfNullData3 = Array(true, true, true)

  val nullablesOfNullData3 = Array(true, false, false)

  lazy val nullData5: Seq[Row] = data5 ++ Seq(
    row(null, 999L, 999, "NullTuple", 999L),
    row(null, 999L, 999, "NullTuple", 999L)
  )

  val nullablesOfNullData5 = Array(true, false, false, false, false)

  lazy val smallTupleData3: Seq[(Int, Long, String)] = {
    val data = new mutable.MutableList[(Int, Long, String)]
    data.+=((1, 1L, "Hi"))
    data.+=((2, 2L, "Hello"))
    data.+=((3, 2L, "Hello world"))
    data
  }

  lazy val smallData3: Seq[Row] = smallTupleData3.map(d => row(d.productIterator.toList: _*))

  val nullablesOfSmallData3 = Array(false, false, false)

  lazy val smallTupleData5: Seq[(Int, Long, Int, String, Long)] = {
    val data = new mutable.MutableList[(Int, Long, Int, String, Long)]
    data.+=((1, 1L, 0, "Hallo", 1L))
    data.+=((2, 2L, 1, "Hallo Welt", 2L))
    data.+=((2, 3L, 2, "Hallo Welt wie", 1L))
    data
  }

  lazy val smallData5: Seq[Row] = smallTupleData5.map(d => row(d.productIterator.toList: _*))

  val nullablesOfSmallData5 = Array(false, false, false, false, false)

  lazy val buildInData: Seq[Row] = Seq(
    row(false, 1.toByte, 2, 3L, 2.56, "abcd", "f%g", localDate("2017-12-12"),
      localTime("10:08:09"), localDateTime("2017-11-11 20:32:19")),

    row(null, 2.toByte, -3, -4L, 90.08, null, "hij_k", localDate("2017-12-12"),
      localTime("10:08:09"), localDateTime("2017-11-11 20:32:19")),

    row(true, 3.toByte, -4, -5L, -0.8, "e fg", null, null,
      localTime("10:08:09"), localDateTime("2015-05-20 10:00:00.887"))
  )

  lazy val simpleData2 = Seq(
    row(1, 0.1),
    row(2, 0.2),
    row(2, 0.2),
    row(3, 0.3),
    row(3, 0.3),
    row(3, 0.4),
    row(4, 0.5),
    row(4, 0.5),
    row(4, 0.6),
    row(4, 0.6),
    row(5, 0.7),
    row(5, 0.7),
    row(5, 0.8),
    row(5, 0.8),
    row(5, 0.9)
  )

  lazy val tupleData3: Seq[(Int, Long, String)] = {
    val data = new mutable.MutableList[(Int, Long, String)]
    data.+=((1, 1L, "Hi"))
    data.+=((2, 2L, "Hello"))
    data.+=((3, 2L, "Hello world"))
    data.+=((4, 3L, "Hello world, how are you?"))
    data.+=((5, 3L, "I am fine."))
    data.+=((6, 3L, "Luke Skywalker"))
    data.+=((7, 4L, "Comment#1"))
    data.+=((8, 4L, "Comment#2"))
    data.+=((9, 4L, "Comment#3"))
    data.+=((10, 4L, "Comment#4"))
    data.+=((11, 5L, "Comment#5"))
    data.+=((12, 5L, "Comment#6"))
    data.+=((13, 5L, "Comment#7"))
    data.+=((14, 5L, "Comment#8"))
    data.+=((15, 5L, "Comment#9"))
    data.+=((16, 6L, "Comment#10"))
    data.+=((17, 6L, "Comment#11"))
    data.+=((18, 6L, "Comment#12"))
    data.+=((19, 6L, "Comment#13"))
    data.+=((20, 6L, "Comment#14"))
    data.+=((21, 6L, "Comment#15"))
    data
  }

  lazy val data3: Seq[Row] = tupleData3.map(d => row(d.productIterator.toList: _*))

  val nullablesOfData3 = Array(false, false, false)

  val nullablesOfData4 = Array(false, false, false)

  val nullablesOfNullData4 = Array(false, false, true)

  lazy val genericData3: Seq[Row] = Seq(
    row(new JTuple2("1", 1), new JTuple2(1, 1), 1),
    row(new JTuple2("2", 1), new JTuple2(1, 1), 2),
    row(new JTuple2("1", 1), new JTuple2(1, 1), 1),
    row(new JTuple2("1", 1), new JTuple2(10, 1), 3)
  )

  val nullablesOfData3WithTimestamp = Array(true, false, false, false)

  lazy val data3WithTimestamp: Seq[Row] = Seq(
    row(2, 2L, "Hello", unixTimestampToLocalDateTime(2000L)),
    row(1, 1L, "Hi", unixTimestampToLocalDateTime(1000L)),
    row(3, 2L, "Hello world", unixTimestampToLocalDateTime(3000L)),
    row(4, 3L, "Hello world, how are you?", unixTimestampToLocalDateTime(4000L)),
    row(5, 3L, "I am fine.", unixTimestampToLocalDateTime(5000L)),
    row(6, 3L, "Luke Skywalker", unixTimestampToLocalDateTime(6000L)),
    row(7, 4L, "Comment#1", unixTimestampToLocalDateTime(7000L)),
    row(8, 4L, "Comment#2", unixTimestampToLocalDateTime(8000L)),
    row(9, 4L, "Comment#3", unixTimestampToLocalDateTime(9000L)),
    row(10, 4L, "Comment#4", unixTimestampToLocalDateTime(10000L)),
    row(11, 5L, "Comment#5", unixTimestampToLocalDateTime(11000L)),
    row(12, 5L, "Comment#6", unixTimestampToLocalDateTime(12000L)),
    row(13, 5L, "Comment#7", unixTimestampToLocalDateTime(13000L)),
    row(15, 5L, "Comment#9", unixTimestampToLocalDateTime(15000L)),
    row(14, 5L, "Comment#8", unixTimestampToLocalDateTime(14000L)),
    row(16, 6L, "Comment#10", unixTimestampToLocalDateTime(16000L)),
    row(17, 6L, "Comment#11", unixTimestampToLocalDateTime(17000L)),
    row(18, 6L, "Comment#12", unixTimestampToLocalDateTime(18000L)),
    row(19, 6L, "Comment#13", unixTimestampToLocalDateTime(19000L)),
    row(20, 6L, "Comment#14", unixTimestampToLocalDateTime(20000L)),
    row(21, 6L, "Comment#15", unixTimestampToLocalDateTime(21000L))
  )

  lazy val smallNestedTupleData: Seq[((Int, Int), String)] = {
      val data = new mutable.MutableList[((Int, Int), String)]
      data.+=(((1, 1), "one"))
      data.+=(((2, 2), "two"))
      data.+=(((3, 3), "three"))
    data
  }

  lazy val tupleData5: Seq[(Int, Long, Int, String, Long)] = {
    val data = new mutable.MutableList[(Int, Long, Int, String, Long)]
    data.+=((1, 1L, 0, "Hallo", 1L))
    data.+=((2, 2L, 1, "Hallo Welt", 2L))
    data.+=((2, 3L, 2, "Hallo Welt wie", 1L))
    data.+=((3, 4L, 3, "Hallo Welt wie gehts?", 2L))
    data.+=((3, 5L, 4, "ABC", 2L))
    data.+=((3, 6L, 5, "BCD", 3L))
    data.+=((4, 7L, 6, "CDE", 2L))
    data.+=((4, 8L, 7, "DEF", 1L))
    data.+=((4, 9L, 8, "EFG", 1L))
    data.+=((4, 10L, 9, "FGH", 2L))
    data.+=((5, 11L, 10, "GHI", 1L))
    data.+=((5, 12L, 11, "HIJ", 3L))
    data.+=((5, 13L, 12, "IJK", 3L))
    data.+=((5, 14L, 13, "JKL", 2L))
    data.+=((5, 15L, 14, "KLM", 2L))
    data
  }

  lazy val data5: Seq[Row] = tupleData5.map(d => row(d.productIterator.toList: _*))

  val nullablesOfData5 = Array(false, false, false, false, false)

  lazy val data6: Seq[Row] = Seq(
    row(1,   1.1, "a",    localDate("2017-04-08"), localTime("12:00:59"),
      localDateTime("2015-05-20 10:00:00")),
    row(2,   2.5, "abc",  localDate("2017-04-09"), localTime("12:00:59"),
      localDateTime("2019-09-19 08:03:09")),
    row(2,  -2.4, "abcd", localDate("2017-04-08"), localTime("00:00:00"),
      localDateTime("2016-09-01 23:07:06")),
    row(3,   0.0, "abc?", localDate("2017-10-11"), localTime("23:59:59"),
      localDateTime("1999-12-12 10:00:00")),
    row(3, -9.77, "ABC",  localDate("2016-08-08"), localTime("04:15:00"),
      localDateTime("1999-12-12 10:00:02")),
    row(3,  0.08, "BCD",  localDate("2017-04-10"), localTime("02:30:00"),
      localDateTime("1999-12-12 10:03:00")),
    row(4,  3.14, "CDE",  localDate("2017-11-11"), localTime("02:30:00"),
      localDateTime("2017-11-20 09:00:00")),
    row(4,  3.15, "DEF",  localDate("2017-02-06"), localTime("06:00:00"),
      localDateTime("2015-11-19 10:00:00")),
    row(4,  3.14, "EFG",  localDate("2017-05-20"), localTime("09:45:78"),
      localDateTime("2015-11-19 10:00:01")),
    row(4,  3.16, "FGH",  localDate("2017-05-19"), localTime("11:11:11"),
      localDateTime("2015-11-20 08:59:59")),
    row(5,  -5.9, "GHI",  localDate("2017-07-20"), localTime("22:22:22"),
      localDateTime("1989-06-04 10:00:00.78")),
    row(5,  2.71, "HIJ",  localDate("2017-09-08"), localTime("20:09:09"),
      localDateTime("1997-07-01 09:00:00.99")),
    row(5,   3.9, "IJK",  localDate("2017-02-02"), localTime("03:03:03"),
      localDateTime("2000-01-01 00:00:00.09")),
    row(5,   0.7, "JKL",  localDate("2017-10-01"), localTime("19:00:00"),
      localDateTime("2010-06-01 10:00:00.999")),
    row(5,  -2.8, "KLM",  localDate("2017-07-01"), localTime("12:00:59"),
      localDateTime("1937-07-07 08:08:08.888"))
  )

  val nullablesOfData6 = Array(false, false, false, false, false, false)

  lazy val duplicateData5: Seq[Row] = Seq(
    row(1, 1L, 10, "Hallo", 1L),
    row(2, 2L, 11, "Hallo Welt", 2L),
    row(2, 3L, 12, "Hallo Welt wie", 1L),
    row(3, 4L, 13, "Hallo Welt wie gehts?", 2L),
    row(3, 5L, 14, "GHI", 2L),
    row(3, 6L, 15, "ABC", 3L),
    row(4, 7L, 16, "ABC", 2L),
    row(4, 8L, 17, "ABC", 1L),
    row(4, 9L, 18, "EFG", 1L),
    row(4, 10L, 19, "EFG", 2L),
    row(5, 11L, 10, "GHI", 1L),
    row(5, 12L, 11, "ABC", 3L),
    row(5, 13L, 12, "IJK", 3L),
    row(5, 14L, 13, "EFG", 2L),
    row(5, 15L, 14, "EFG", 2L)
  )

  val nullablesOfDuplicateData5 = Array(false, false, false, false, false)

  lazy val numericData: Seq[Row] = Seq(
    row(1, 1L, 1.0f, 1.0d, JBigDecimal.valueOf(1)),
    row(2, 2L, 2.0f, 2.0d, JBigDecimal.valueOf(2)),
    row(3, 3L, 3.0f, 3.0d, JBigDecimal.valueOf(3))
  )

  val nullablesOfNumericData = Array(false, false, false, false, false)

  // person test data
  lazy val personData: Seq[Row] = Seq(
    row(1, 23, "tom", 172, "m"),
    row(2, 21, "mary", 161, "f"),
    row(3, 18, "jack", 182, "m"),
    row(4, 25, "rose", 165, "f"),
    row(5, 27, "danny", 175, "m"),
    row(6, 31, "tommas", 172, "m"),
    row(7, 19, "olivia", 172, "f"),
    row(8, 34, "stef", 170, "m"),
    row(9, 32, "emma", 171, "f"),
    row(10, 28, "benji", 165, "m"),
    row(11, 20, "eva", 180, "f")
  )

  val nullablesOfPersonData = Array(false, false, false, false, false)
  val personType = new RowTypeInfo(INT_TYPE_INFO, INT_TYPE_INFO, STRING_TYPE_INFO,
    INT_TYPE_INFO, STRING_TYPE_INFO)

  val INT_DOUBLE = new RowTypeInfo(INT_TYPE_INFO, DOUBLE_TYPE_INFO)
  val INT_STRING = new RowTypeInfo(INT_TYPE_INFO, STRING_TYPE_INFO)
  val INT_ONLY = new RowTypeInfo(INT_TYPE_INFO)
  val INT_INT = new RowTypeInfo(INT_TYPE_INFO, INT_TYPE_INFO)

  lazy val data2_1: Seq[Row] = Seq(
    row(1, 2.0),
    row(1, 2.0),
    row(2, 1.0),
    row(2, 1.0),
    row(3, 3.0),
    row(null, null),
    row(null, 5.0),
    row(6, null)
  )

  lazy val data2_2: Seq[Row] = Seq(
    row(2, 3.0),
    row(2, 3.0),
    row(3, 2.0),
    row(4, 1.0),
    row(null, null),
    row(null, 5.0),
    row(6, null)
  )

  lazy val data2_3: Seq[Row] = Seq(
    row(2, 3.0),
    row(2, 3.0),
    row(3, 2.0),
    row(4, 1.0)
  )

  val nullablesOfData2_3 = Array(false, false)

  lazy val intStringData: Seq[Row] = {
    (1 to 100).map(i => row(i, i.toString))
  }

  val nullablesOfIntStringData = Array(false, false)

  lazy val bigIntStringData: Seq[Row] = {
    (1 to 10000).map(i => row(i, i.toString))
  }

  lazy val intIntData2: Seq[Row] = {
    row(1, 1) ::
        row(1, 2) ::
        row(2, 1) ::
        row(2, 2) ::
        row(3, 1) ::
        row(3, 2) :: Nil
  }

  val nullablesOfIntIntData2 = Array(false, false)

  lazy val intIntData3: Seq[Row] = {
    row(1, null) ::
        row(2, 2) :: Nil
  }

  val nullablesOfIntIntData3 = Array(false, false)

  lazy val upperCaseData: Seq[Row] = Seq(
    row(1, "A"),
    row(2, "B"),
    row(3, "C"),
    row(4, "D"),
    row(5, "E"),
    row(6, "F"))

  val nullablesOfUpperCaseData = Array(false, false)

  lazy val lowerCaseData: Seq[Row] = Seq(
    row(1, "a"),
    row(2, "b"),
    row(3, "c"),
    row(4, "d"))

  val nullablesOfLowerCaseData = Array(false, false)

  lazy val allNulls: Seq[Row] = Seq(
    row(null),
    row(null),
    row(null),
    row(null))

  val nullablesOfAllNulls = Array(true)

  lazy val projectionTestData: Seq[Row] = Seq(
    row(1, 10, 100, "1", "10", "100", 1000, "1000"),
    row(2, 20, 200, "2", "20", "200", 2000, "2000"),
    row(3, 30, 300, "3", "30", "300", 3000, "3000"))

  val projectionTestDataType =
    new RowTypeInfo(
      INT_TYPE_INFO, INT_TYPE_INFO, INT_TYPE_INFO,
      STRING_TYPE_INFO, STRING_TYPE_INFO, STRING_TYPE_INFO,
      INT_TYPE_INFO, STRING_TYPE_INFO)

  val nullablesOfProjectionTestData = Array(false, false, false, false, false, false, false, false)
}
