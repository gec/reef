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
package org.totalgrid.reef.client.sapi.rpc.impl.util

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.loader.commons.{ ModelDeleter, LoaderServices }
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection

object ModelPreparer {
  var lastModelFile = ""

  def load(modelFile: String, client: Client, waitForEndpoints: Boolean) {
    load(modelFile, client.getService(classOf[LoaderServices]), client.getService(classOf[AllScadaService]), waitForEndpoints)
  }

  private def load(modelFile: String, loaderServices: LoaderServices, scadaServices: AllScadaService, waitForEndpoints: Boolean) {
    if (lastModelFile != modelFile) {
      lastModelFile = modelFile
      ModelDeleter.deleteEverything(loaderServices, false, true, Some(Console.out))
      LoadManager.loadFile(loaderServices, modelFile, false, false, false)
      // wait for all endpoints to be up before continuing
      if (waitForEndpoints) waitForEndpointsOnline(scadaServices)
    }
  }

  def waitForEndpointsOnline(scadaServices: AllScadaService) {
    val result = scadaServices.subscribeToEndpointConnections().await
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.COMMS_UP)
  }
}