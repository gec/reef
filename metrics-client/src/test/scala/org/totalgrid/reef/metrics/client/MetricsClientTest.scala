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
package org.totalgrid.reef.metrics.client

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.settings.AmqpSettings
import org.totalgrid.reef.client.sapi.client.factory.ReefFactory
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

@RunWith(classOf[JUnitRunner])
class MetricsClientTest extends FunSuite with ShouldMatchers {

  /*
  test("test") {
    val props = PropertyReader.readFromFile("../org.totalgrid.reef.amqp.cfg")
    val config = new AmqpSettings(props)

    val factory = new ReefFactory(config, new ReefServices)

    val conn = factory.connect()
    conn.addServicesList(new MetricsServiceList)
    val client = conn.login("system", "system").await

    val metrics = client.getRpcInterface(classOf[MetricsService])

    println(metrics.getMetrics())

    factory.terminate()
  }*/
}