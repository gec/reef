/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measurementstore.encoders

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.{ Reporter, Stopper, Filter, Distributor, Tracker }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.util.Random

import org.totalgrid.reef.util.Timing
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas, Quality }

@RunWith(classOf[JUnitRunner])
class CompressionTest extends FunSuite with ShouldMatchers {

  val block_size = 1000

  def getMeas(time: Long, value: Long) = {
    val meas = Meas.newBuilder
    meas.setType(Meas.Type.INT).setIntVal(value)
    meas.setQuality(Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  trait RandomMeas {
    val rand = new Random(0)
    var time: Long = 0

    def nextMeas(): Meas.Builder

    def next() = {
      val ret = nextMeas
      time += rand.nextInt(2000)
      ret.setTime(time)
      ret.setQuality(Quality.newBuilder.build)
      ret.build
    }
  }

  class RandomInt extends RandomMeas {
    var value = Long.MaxValue / 4
    def nextMeas() = {
      value += rand.nextInt(100) - 50
      Meas.newBuilder.setIntVal(value).setType(Meas.Type.INT)
    }
  }

  class RandomDouble extends RandomMeas {
    var value = 100.0
    def nextMeas() = {
      value += ((rand.nextDouble - 0.5) * 0.2 * value) // change by no more than 10% of current value      
      Meas.newBuilder.setDoubleVal(value).setType(Meas.Type.DOUBLE)
    }
  }

  // test the encode/decode correctness  
  def testAndBenchmark(encoder: MeasEncoder, name: String, input: Seq[Meas]) = {
    val predeflate = input.foldLeft(0) { (sum, m) => sum + m.getSerializedSize }.toDouble

    //val size = Timing.time(name) {
    val bytes = encoder.encode(input)
    val output = encoder.decode(bytes)
    output should equal(input)
    //bytes.length
    //}.toDouble

    //println("%2.1f".format(size/predeflate*100) + "% of original size: " + size/input.length + " bytes/meas ")

  }

  def generate(measgen: RandomMeas) =
    for (i <- 1 to block_size) yield measgen.next()

  def testEncodings(name: String, input: Seq[Meas]) {
    testAndBenchmark(new SimpleMeasEncoder, name + " w/ Simple", input)
    testAndBenchmark(new SimpleMeasEncoder with JavaZipping, name + " w/ SimpleWithZipping", input)
    testAndBenchmark(new NullMeasEncoder, name + " w/ NullMeas", input)
    testAndBenchmark(new NullMeasEncoder with JavaZipping, name + " w/ NullMeasWithZipping", input)
  }

  test("IntEncodings") {
    testEncodings("BrownianInts", generate(new RandomInt))
  }

  test("DoubleEncodings") {
    testEncodings("BrownianDouble", generate(new RandomDouble))
  }

  test("IntegerEdgeCases") {
    def get(value: Long) = {
      Meas.newBuilder.setIntVal(value)
        .setType(Meas.Type.INT).setTime(0)
        .setQuality(Quality.newBuilder.build).build
    }

    val values = List(Long.MaxValue, Long.MinValue, Long.MaxValue).map { get _ }
    testEncodings("IntegerEdgeCases", values)
  }

  test("RandomLong") {
    def get(value: Long) = {
      Meas.newBuilder.setIntVal(value)
        .setType(Meas.Type.INT).setTime(0)
        .setQuality(Quality.newBuilder.build).build
    }

    val r = new Random(0)
    val randlist = for (i <- 1 to block_size) yield get(r.nextLong)
    testEncodings("RandomIntegers", randlist)
  }

}