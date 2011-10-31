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
package org.totalgrid.reef.protocol.dnp3.integrationtests

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.ReefFactory
import org.totalgrid.reef.clientapi.settings.util.PropertyReader
import org.totalgrid.reef.loader.commons.{ LoaderClient, LoaderServices, ModelDeleter }
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import org.totalgrid.reef.clientapi.settings.{ UserSettings, AmqpSettings }
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.totalgrid.reef.proto.FEP.CommEndpointConnection
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.clientapi.{ SubscriptionEvent, SubscriptionEventAcceptor, SubscriptionResult }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.proto.FEP.CommEndpointConnection.State._
import com.weiglewilczek.slf4s.Logging

@RunWith(classOf[JUnitRunner])
class Dnp3StartStopIT extends FunSuite with ShouldMatchers with BeforeAndAfterAll with Logging {

  val stream = Some(Console.out)

  var factoryOption = Option.empty[ReefFactory]
  var clientOption = Option.empty[Client]
  def client = clientOption.get
  def loaderServices = client.getRpcInterface(classOf[LoaderServices])
  def services = client.getRpcInterface(classOf[AllScadaService])

  override def beforeAll() {
    val props = PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg")
    val amqp = new AmqpSettings(props)
    val user = new UserSettings(props)

    factoryOption = Some(new ReefFactory(amqp))

    val connection = factoryOption.get.connect()
    val client = connection.login(user.getUserName, user.getUserPassword).await

    LoaderClient.prepareClient(client)

    clientOption = Some(client)
  }

  override def afterAll() {
    factoryOption.foreach { _.terminate() }
  }

  test("Clear system") {
    ModelDeleter.deleteEverything(loaderServices, false, stream)
  }

  test("Load dnp3 model") {
    LoadManager.loadFile(loaderServices, "src/test/resources/sample-model.xml", false, false, false)
  }

  test("Cycle endpoints") {
    val endpoints = services.getAllEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    val result = services.subscribeToAllEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    (1 to 5).foreach { i =>

      val start = System.currentTimeMillis()
      endpoints.foreach { e => services.disableEndpointConnection(e.getUuid).await }

      map.checkAllState(false, COMMS_DOWN)
      val disabled = System.currentTimeMillis()
      println("Disabled to COMMS_DOWN in: " + (disabled - start))

      endpoints.foreach { e => services.enableEndpointConnection(e.getUuid).await }

      map.checkAllState(true, COMMS_UP)
      println("Enabled to COMMS_UP in: " + (System.currentTimeMillis() - disabled))
    }
  }

  class EndpointConnectionStateMap(result: SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]) {

    private def makeEntry(e: CommEndpointConnection) = {
      //println(e.getEndpoint.getName + " s: " + e.getState + " e: " + e.getEnabled + " a:" + e.getFrontEnd.getUuid.getUuid + " at: " + e.getLastUpdate)
      e.getEndpoint.getUuid -> e
    }

    val endpointStateMap = result.getResult.map { makeEntry(_) }.toMap
    val syncVar = new SyncVar(endpointStateMap)

    result.getSubscription.start(new SubscriptionEventAcceptor[CommEndpointConnection] {
      def onEvent(event: SubscriptionEvent[CommEndpointConnection]) {
        syncVar.atomic(m => m + makeEntry(event.getValue))
      }
    })

    def checkAllState(enabled: Boolean, state: CommEndpointConnection.State) {
      syncVar.waitFor(x => x.values.forall(e => e.getEnabled == enabled && e.getState == state), 20000)
    }
    def checkState(uuid: ReefUUID, enabled: Boolean, state: CommEndpointConnection.State) {
      syncVar.waitFor(x => x.get(uuid).map(e => e.getEnabled == enabled && e.getState == state).getOrElse(false), 20000)
    }
  }
}