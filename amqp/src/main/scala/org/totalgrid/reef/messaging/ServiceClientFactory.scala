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
package org.totalgrid.reef.messaging

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.protoapi.ProtoServiceTypes.Event
import org.totalgrid.reef.protoapi.client.ServiceClient
import javabridge.Subscription

/**
 * factory trait that defines what we need to construct ServiceClients and subscriptions
 */
trait ServiceClientFactory {

  /**
   * the factory must create and start a ServiceResponseCorrelator that will be shared by all clients
   */
  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator

  /**
   * the factory must create subscription objects of the appropriate type even if its a "stream type"
   */
  def prepareSubscription[T <: GeneratedMessage](deserialize: Array[Byte] => T, subIsStreamType: Boolean, callback: Event[T] => Unit): Subscription

  /**
   * gets a single proto client with its own correlator
   */
  def getProtoServiceClient[T <: GeneratedMessage](exchange: String, key: String, timeoutms: Long, deserialize: Array[Byte] => T): ServiceClient = {
    val correlator = getServiceResponseCorrelator(timeoutms)
    addProtoServiceClient(exchange, key, deserialize, correlator)
  }

  /**
   *  gets a proto client with default key "request"
   */
  def getProtoServiceClient[T <: GeneratedMessage](exchange: String, timeoutms: Long, deserialize: Array[Byte] => T): ServiceClient = {
    getProtoServiceClient(exchange, "request", timeoutms, deserialize)
  }

  /**
   * adds a proto client to a single ServiceResponseCorrelator
   */
  def addProtoServiceClient[T <: GeneratedMessage](exchange: String, key: String, deserialize: Array[Byte] => T, correlator: ServiceResponseCorrelator): ServiceClient = {
    new ProtoServiceClient[T](deserialize, exchange, key, correlator)
  }

  /**
   * adds a proto client to a single ServiceResponseCorrelator with default key "request"
   */
  def addProtoServiceClient[T <: GeneratedMessage](exchange: String, deserialize: Array[Byte] => T, correlator: ServiceResponseCorrelator): ServiceClient = {
    addProtoServiceClient(exchange, "request", deserialize, correlator)
  }

}