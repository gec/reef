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
package org.totalgrid.reef.benchmarks.endpoints

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import java.io.PrintStream
import org.totalgrid.reef.proto.FEP.CommEndpointConnection._
import org.totalgrid.reef.benchmarks.{ BenchmarkReading, FailedBenchmarkException, BenchmarkTest }

case class EndpointCycleReading(endpointName: String, protocol: String, finalState: State, stateTransitionTime: Long) extends BenchmarkReading {
  def csvName = "endpoint"

  def testOutputNames = "StateTransmissionTime" :: Nil

  def testOutputs = stateTransitionTime :: Nil

  def testParameterNames = "protocol" :: "finalState" :: Nil

  def testParameters = protocol :: finalState :: Nil
}

class EndpointManagementBenchmark(endpointNames: List[String], cycles: Int) extends BenchmarkTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {
    val endpoints = endpointNames.map { client.getEndpointByName(_).await }

    if (endpoints.isEmpty) throw new FailedBenchmarkException("No endpoints to cycle")

    val result = client.subscribeToAllEndpointConnections().await

    val map = new EndpointStateTransitionTimer(result, endpoints.map { _.getUuid })

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, State.COMMS_UP)

    (1 to cycles).map { i =>

      map.start
      endpoints.map { e => client.disableEndpointConnection(e.getUuid) }.foreach { _.await }

      map.checkAllState(false, State.COMMS_DOWN)
      val results = map.getStateReadings

      map.start
      endpoints.map { e => client.enableEndpointConnection(e.getUuid) }.foreach { _.await }

      map.checkAllState(true, State.COMMS_UP)

      results ::: map.getStateReadings
    }.toList.flatten
  }
}