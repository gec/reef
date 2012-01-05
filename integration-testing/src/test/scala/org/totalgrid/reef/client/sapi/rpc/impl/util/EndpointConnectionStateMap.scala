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

import org.totalgrid.reef.client.SubscriptionResult
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.client.service.proto.Model.ReefUUID

class EndpointConnectionStateMap(result: SubscriptionResult[List[EndpointConnection], EndpointConnection]) {

  private def makeEntry(e: EndpointConnection) = {
    //println(e.getEndpointByUuid.getName + " s: " + e.getState + " e: " + e.getEnabled + " a:" + e.getFrontEnd.getUuid.getUuid + " at: " + e.getLastUpdate)
    e.getEndpoint.getUuid -> e
  }

  val endpointStateMap = result.getResult.map { makeEntry(_) }.toMap
  val syncVar = new SyncVar(endpointStateMap)

  result.getSubscription.start(new SubscriptionEventAcceptorShim(ea => syncVar.atomic(m => m + makeEntry(ea.getValue))))

  def checkAllState(enabled: Boolean, state: EndpointConnection.State) {
    syncVar.waitFor(x => x.values.forall(e => e.getEnabled == enabled && e.getState == state), 20000)
  }
  def checkState(uuid: ReefUUID, enabled: Boolean, state: EndpointConnection.State) {
    syncVar.waitFor(x => x.get(uuid).map(e => e.getEnabled == enabled && e.getState == state).getOrElse(false), 20000)
  }
}