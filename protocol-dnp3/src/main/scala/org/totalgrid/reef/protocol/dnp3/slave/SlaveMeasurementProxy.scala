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
package org.totalgrid.reef.protocol.dnp3.slave

import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.Mapping.{ IndexMapping }
import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.protocol.dnp3._
import com.weiglewilczek.slf4s.Logging

import org.totalgrid.reef.clientapi.{ SubscriptionEvent, SubscriptionEventAcceptor, Subscription }

import org.totalgrid.reef.client.sapi.rpc.AllScadaService

class SlaveMeasurementProxy(service: AllScadaService, mapping: IndexMapping, dataObserver: IDataObserver)
    extends SubscriptionEventAcceptor[Measurement] with Logging {

  private val publisher = new DataObserverPublisher(mapping, dataObserver)
  private val packTimer = new PackTimer(100, 400, publisher.publishMeasurements _, service)

  private var subscription: Option[Subscription[_]] = None

  service.execute {
    val subscriptionResult = service.subscribeToMeasurementsByNames(mapping.getMeasmapList.toList.map { _.getPointName }).await
    subscription = Some(subscriptionResult.getSubscription)
    packTimer.addEntries(subscriptionResult.getResult.toList)
    subscriptionResult.getSubscription.start(this)
  }

  def stop() {
    subscription.foreach { _.cancel }
    packTimer.cancel()
  }

  def onEvent(event: SubscriptionEvent[Measurement]) {
    packTimer.addEntry { event.getValue }
  }

}