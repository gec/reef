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
package org.totalgrid.reef.benchmarks

import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.settings.{ AmqpSettings, UserSettings }
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.client.factory.ReefConnectionFactory

object AllBenchmarksEntryPoint {
  def main(args: Array[String]) {

    val (configFiles, benchmarkFile) = parseArgs(args.toList)

    val properties = PropertyReader.readFromFiles(configFiles)

    val testOptions = PropertyReader.readFromFile(benchmarkFile)

    val userSettings = new UserSettings(properties)
    val connectionInfo = new AmqpSettings(properties)

    val factory = ReefConnectionFactory.buildFactory(connectionInfo, new ReefServices)

    try {

      val connection = factory.connect()
      val client = connection.login(userSettings)

      BenchmarksRunner.runAllTests(connection, client, testOptions)

    } finally {
      factory.terminate()
    }
  }

  private def usage: Unit = {

    println("usage: AllBenchmarkEntryPoint -configFile target.cfg -c benchmarkSettings.cfg")
    println("Last argument is benchmarks config file")
    println("OPTIONS:")
    println("  -configFile <userfile> Path to *.cfg file(s)")
    println("  -c benchmark run cfg file")
    println("")
    java.lang.System.exit(-1)
  }

  private def more(args: List[String]): List[String] = {
    val args2 = args drop 1
    if (args2.isEmpty)
      usage
    args2
  }

  import scala.collection.JavaConversions._
  private def parseArgs(inputArgs: List[String]): (java.util.List[String], String) = {

    var configFiles = List.empty[String]
    var benchmarkFile = "assemblies/assembly-common/filtered-resources/etc/org.totalgrid.reef.benchmarks.cfg"

    var args = inputArgs
    try {
      while (!args.isEmpty) {
        args.head match {
          case "-c" =>
            args = more(args)
            benchmarkFile = args.head
          case "--configFile" =>
            args = more(args)
            configFiles = List(args.head) ::: configFiles
        }
        args = args drop 1
      }
    } catch {
      case ex =>
        printf("Exception: " + ex.toString)
        usage
    }

    if (configFiles.isEmpty) configFiles = List("target.cfg")

    (configFiles, benchmarkFile)
  }
}
