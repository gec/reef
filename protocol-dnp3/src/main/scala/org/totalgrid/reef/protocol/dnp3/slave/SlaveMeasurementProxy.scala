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

import org.totalgrid.reef.client.service.proto.Mapping.{ IndexMapping }
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.protocol.dnp3._
import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.app.{ ServiceContext, SubscriptionDataHandler }
import net.agileautomata.executor4s.{ Strand, Executor, Cancelable }
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.protocol.api.util.PackTimer

class SlaveMeasurementProxy(service: AllScadaService, mapping: IndexMapping, dataObserver: IDataObserver, exe: Executor)
    extends SubscriptionDataHandler[Measurement] with Logging {

  private def measList = mapping.getMeasmapList.toList

  private val publisher = new DataObserverPublisher(mapping, dataObserver)
  private val packTimer = new PackTimer(100, 400, publisher.publishMeasurements _, Strand(exe))
  private val scaler = new MeasurementOutputScaler(measList)

  private var subscription = Option.empty[Cancelable]

  exe.execute {
    service.subscribeToMeasurementsByNames(measList.map { _.getPointName }).listenFor { p =>
      val subscriptionResult = p.await
      subscription = Some(ServiceContext.attachToServiceContext(subscriptionResult, this))
    }
  }

  def stop() {
    subscription.foreach { _.cancel() }
    packTimer.cancel()
  }

  def handleResponse(result: List[Measurement]) = packTimer.addEntries(result.map { scaler.scaleMeasurement(_) })
  def handleEvent(event: SubscriptionEventType, result: Measurement) = packTimer.addEntry(scaler.scaleMeasurement(result))

}