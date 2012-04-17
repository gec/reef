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

import java.io.PrintStream
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

/**
 * Each concrete benchmark test will implement this interface. The classes will be constructed
 * with any input parmeters ahead of time. It is assumed that tests will be run serially
 * and will leave the system in a good state if at all possible.
 */
trait BenchmarkTest {
  def runTest(client: Client, stream: Option[PrintStream]): List[BenchmarkReading]
}

/**
 * benchmarks that use AllScadaServices can use this base trait to avoid getRpcInterface calls
 */
trait AllScadaServicesTest extends BenchmarkTest {
  def runTest(client: Client, stream: Option[PrintStream]) = {
    runTest(client.getService(classOf[AllScadaService]), stream)
  }
  def runTest(client: AllScadaService, stream: Option[PrintStream]): List[BenchmarkReading]
}

/**
 * each run from a benchmark test will certain fields that can be handled generically for
 * output and simple analysis.
 */
trait BenchmarkReading {

  /**
   * name of the output type, useful so we can group like types of outputs
   */
  def csvName: String

  /**
   * names and values for the input parameters to each test run, a different set of paramters
   * is considered to be different enough from other runs so the results are not checked against
   * other similar tests because they are expected to be very different.
   *
   * names shall match the values in testParameters
   */
  def testParameterNames: List[String]
  def testParameters: List[Any]

  /**
   * these are the measured outputs from the test runs that are expected to vary between runs.
   * They will be collected and analyzed with other readings that had same parameters
   */
  def testOutputNames: List[String]
  def testOutputs: List[Any]

  def columnNames: List[String] = testParameterNames ::: testOutputNames
  def values: List[Any] = testParameters ::: testOutputs

  def outputsWithLabels: List[(String, Any)] = testOutputNames.zip(testOutputs)
  def parametersWithLabels: List[(String, Any)] = testParameterNames.zip(testParameters)
}