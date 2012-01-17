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
package org.totalgrid.reef.benchmarks.system

import org.totalgrid.reef.benchmarks._
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import java.io.PrintStream
import collection.mutable.Queue
import org.totalgrid.reef.util.Timing

import scala.collection.JavaConversions._

case class SystemStat(statName: String, value: Long) extends BenchmarkReading {
  def csvName = "systemStats"

  // just need a test parameterName
  def testParameterNames = List("stat")
  def testParameters = List(statName)

  def testOutputNames = List("value")
  def testOutputs = List(value)
}

case class SystemTimingStat(request: String, time: Long) extends BenchmarkReading {
  def csvName = "systemRequests"

  // just need a test parameterName
  def testParameterNames = List("request")
  def testParameters = List(request)

  def testOutputNames = List("time")
  def testOutputs = List(time)
}

class SystemStateBenchmark(runs: Int) extends AllScadaServicesTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    val readings = Queue.empty[BenchmarkReading]

    def time[A](name: String)(fun: => List[A]): List[A] = {
      Timing.time { t =>
        readings.enqueue(new SystemTimingStat(name, t))
        // make a generic recording for all requests
        readings.enqueue(new SystemTimingStat("request", t))
      } {
        stream.foreach { _.println("Requesting " + name) }

        val list = fun

        readings.enqueue(new SystemStat(name, list.size))

        list
      }
    }

    (0 to runs).foreach { i =>

      val endpoints = time("allEndpoints") { client.getEndpoints().await }

      endpoints.foreach { e =>
        readings.enqueue(new SystemStat("endpointPoints", e.getOwnerships.getPointsCount))
        readings.enqueue(new SystemStat("endpointCommands", e.getOwnerships.getPointsCount))
      }
      endpoints.groupBy { _.getProtocol }.foreach {
        case (protocol, eps) =>
          readings.enqueue(new SystemStat("endpointProtocol" + protocol, eps.size))
          readings.enqueue(new SystemStat("endpointProtocol", eps.size))
      }

      time("allPoints") { client.getPoints().await }
      time("allCommands") { client.getCommands().await }
      //time("allCommandHistory"){client.getCommandHistory().await}
      time("allConfigFiles") { client.getConfigFiles().await }
      time("allEntities") { client.getEntities().await }
      time("allAgents") { client.getAgents().await }
      time("allPermissionSets") { client.getPermissionSets().await }
      val applications = time("allApplications") { client.getApplications().await }

      applications.groupBy { _.getCapabilitesList.toList.headOption.getOrElse("none") }.foreach {
        case (capability, apps) =>
          readings.enqueue(new SystemStat("appsCapability" + capability, apps.size))
          readings.enqueue(new SystemStat("appsCapability", apps.size))
      }
      applications.foreach { app =>
        readings.enqueue(new SystemStat("appOnline", if (app.getOnline) 1 else 0))
      }
    }

    readings.toList
  }
}