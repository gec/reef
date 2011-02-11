/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging.serviceprovider

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.protoapi.ServiceList

/**
 * mixin that manages the lazy creation and storing of subscription handlers (publishers) 
 * that the models use. This way the models can be constructed multiple times and all publications
 * go through the same publishingactors  
 */
abstract class ServiceEventPublisherMap(lookup: ServiceList) extends ServiceEventPublishers {

  /**
   * concrete implementations define this to inject a specific ServiceSubscriptionHandler class
   */
  def createPublisher(exchange: String): ServiceSubscriptionHandler

  private var built = Map.empty[String, ServiceSubscriptionHandler]

  // Lazy instantiation of sub handlers
  private def getSink(exch: String): ServiceSubscriptionHandler = {
    built.get(exch).getOrElse {
      val pubsub = createPublisher(exch)
      built += (exch -> pubsub)
      pubsub
    }
  }
  def getEventSink[A <: GeneratedMessage](klass: Class[A]): ServiceSubscriptionHandler = {
    getSink(lookup.getServiceInfo(klass).subExchange)
  }
}
