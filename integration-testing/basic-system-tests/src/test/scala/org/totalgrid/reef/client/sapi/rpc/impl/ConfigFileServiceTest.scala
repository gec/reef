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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import scala.util.Random

/**
 * example of the live example documentation/tests we use to make it clear exactly how the system is implementing
 * the queries and handling the responses. Written exclusively with the high level apis.
 */
@RunWith(classOf[JUnitRunner])
class ConfigFileServiceTest extends ServiceClientSuite {

  test("Create config files") {
    val cf = client.createConfigFile("Test-Config-File", "text/plain", "Data".getBytes())

    client.updateConfigFile(cf, "New Data".getBytes())

    client.deleteConfigFile(cf)
  }

  test("Associate Config File to Entity") {
    val entity = client.getEntityByName("StaticSubstation")

    val cf1 = client.createConfigFile("Test-Entity-Text-File", "text/plain", "Data".getBytes(), entity.getUuid)
    val cf2 = client.createConfigFile("Test-Entity-XML-File", "text/xml", "<Data/>".getBytes(), entity.getUuid)

    client.getConfigFilesUsedByEntity(entity.getUuid)

    client.getConfigFilesUsedByEntity(entity.getUuid, "text/xml")

    val entity2 = client.getEntityByName("SimulatedSubstation")
    client.addConfigFileUsedByEntity(cf1, entity2.getUuid)

    client.deleteConfigFile(cf1)
    client.deleteConfigFile(cf2)
  }

  test("Create config file with hard to serialize bytes") {

    val bytes = new Array[Byte](10000)
    val rand = new Random()
    rand.nextBytes(bytes)
    val cf = client.createConfigFile("Test-Config-File", "text/plain", bytes)

    cf.getFile.toByteArray should equal(bytes)

    client.deleteConfigFile(cf)
  }
}