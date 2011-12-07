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

import org.totalgrid.reef.loader.LoadManager

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite
import org.totalgrid.reef.loader.commons.{ LoaderServices, LoaderClient, ModelDeleter }
import org.totalgrid.reef.util.Timing

@RunWith(classOf[JUnitRunner])
class ModelSetup extends ClientSessionSuite("Setup.xml", "Model Setup", <div></div>) {

  test("Delete model") {
    LoaderClient.prepareClient(session)

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])
    loaderServices.setHeaders(loaderServices.getHeaders.setTimeout(50000))

    ModelDeleter.deleteEverything(loaderServices, false, Some(Console.out))
  }

  test("Load mainstreet model") {

    LoaderClient.prepareClient(session)

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])

    val fileName = "../assemblies/assembly-common/filtered-resources/samples/mainstreet/config.xml"
    loaderServices.setHeaders(loaderServices.getHeaders.setTimeout(50000))

    LoadManager.loadFile(loaderServices, fileName, true, false, false, 25)
  }

  test("Delete mainstreet model") {
    LoaderClient.prepareClient(session)

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])
    loaderServices.setHeaders(loaderServices.getHeaders.setTimeout(50000))

    ModelDeleter.deleteEverything(loaderServices, false, Some(Console.out))
  }

  test("Load integration model") {

    LoaderClient.prepareClient(session)

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])

    val fileName = "../assemblies/assembly-common/filtered-resources/samples/integration/config.xml"

    loaderServices.setHeaders(loaderServices.getHeaders.setTimeout(50000))

    Timing.time("No batching") {
      LoadManager.loadFile(loaderServices, fileName, true, false, false, 0)
    }
    ModelDeleter.deleteEverything(loaderServices, false, Some(Console.out))
    Timing.time("25 entry batch") {
      LoadManager.loadFile(loaderServices, fileName, false, false, false, 25)
    }
  }
}