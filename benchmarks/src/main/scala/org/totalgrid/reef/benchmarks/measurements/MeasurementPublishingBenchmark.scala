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
package org.totalgrid.reef.benchmarks.measurements

import org.totalgrid.reef.client.sapi.rpc.AllScadaService

import scala.collection.JavaConversions._
import org.totalgrid.reef.clientapi.{ AnyNodeDestination, AddressableDestination }
import org.totalgrid.reef.util.Timing
import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.benchmarks._
import java.io.PrintStream

class MeasurementPublishingBenchmark(endpointNames: List[String], measCount: Int, attempts: Int, direct: Boolean) extends BenchmarkTest {

  case class Reading(endpointName: String, direct: Boolean, measurements: Long, publishTime: Long, firstMessageTime: Long, roundtripTime: Long) extends BenchmarkReading {

    def csvName = "measurementPublishing"

    def testParameterNames = "directPublishing" :: "numberOfMeasurements" :: Nil
    def testOutputNames = "publishingTime" :: "firstMessageTime" :: "lastMessageTime" :: Nil

    def testParameters: List[Any] = {
      direct :: measurements :: Nil
    }
    def testOutputs: List[Any] = {
      publishTime :: firstMessageTime :: roundtripTime :: Nil
    }
  }

  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    stream.foreach(_.println("Running MeasurementPublishingTests: " + attempts))

    endpointNames.map { testEndpoint(_, client) }.flatten
  }

  private def testEndpoint(endpointName: String, client: AllScadaService) = {
    val endpoint = client.getEndpointByName(endpointName).await

    val names = endpoint.getOwnerships.getPointsList.toList
    val destination = if (direct) {
      val connection = client.getEndpointConnectionByUuid(endpoint.getUuid).await
      new AddressableDestination(connection.getRouting.getServiceRoutingKey)
    } else {
      new AnyNodeDestination()
    }

    val sub = client.subscribeToMeasurementsByNames(names).await
    val originals = sub.getResult

    val handler = new MeasurementRoundtripTimer(sub)

    (1 to attempts).map { i =>

      val toPublish = updateMeasurements(originals, measCount, System.currentTimeMillis() + i)

      handler.start(toPublish)
      val pubTime = Timing.benchmark {
        client.publishMeasurements(toPublish, destination).await
      }
      val roundtripTime = handler.await

      Reading(endpointName, direct, measCount, pubTime, handler.firstMessage.get, roundtripTime)
    }.toList
  }

  private def updateMeasurements(originals: List[Measurement], size: Int, nowMillis: Long) = {
    Stream.continually(originals).flatten.take(size).toList.map { m =>
      if (m.getType == Measurement.Type.DOUBLE)
        m.toBuilder.setDoubleVal(m.getDoubleVal + 1.0).setTime(nowMillis).build
      else if (m.getType == Measurement.Type.BOOL)
        m.toBuilder.setBoolVal(!m.getBoolVal).setTime(nowMillis).build
      else if (m.getType == Measurement.Type.INT)
        m.toBuilder.setIntVal(m.getIntVal + 1).setTime(nowMillis).build
      else m.toBuilder.setTime(nowMillis).build
    }
  }
}