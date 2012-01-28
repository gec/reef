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
package org.totalgrid.reef.client.sapi.client.rest.impl

import net.agileautomata.executor4s._
import org.totalgrid.reef.client.sapi.client.rest.{ Client }
import org.totalgrid.reef.client.sapi.client.{ RequestSpyHook, BasicRequestHeaders }
import org.totalgrid.reef.client.proto.Envelope.{ SubscriptionEventType, Verb }
import org.totalgrid.reef.client.sapi.service.AsyncService

import org.totalgrid.reef.client.types.{ ServiceTypeInformation, TypeDescriptor }
import org.totalgrid.reef.client.{ ServiceProviderInfo, ServicesList, Routable }
import org.totalgrid.reef.client.{ ServicesList, ServiceProviderInfo }
import org.totalgrid.reef.client.settings.UserSettings

class DefaultClient(conn: DefaultConnection, strand: Strand) extends Client with RequestSpyHook with ExecutorDelegate {

  protected def executor = strand

  override def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {
    val usedHeaders = headers.map { getHeaders.merge(_) }.getOrElse(getHeaders)
    val future = conn.request(verb, payload, usedHeaders, strand)
    notifyRequestSpys(verb, payload, future)
    future
  }

  final override def subscribe[A](descriptor: TypeDescriptor[A]) = {
    notifySubscriptionCreated(conn.subscribe(strand, descriptor))
  }
  final override def bindService[A](service: AsyncService[A], dispatcher: Executor, destination: Routable, competing: Boolean) = {
    notifySubscriptionCreated(conn.bindService(service, dispatcher, destination, competing))
  }
  final override def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) = conn.bindQueueByClass(subQueue, key, klass)
  final override def publishEvent[A](typ: SubscriptionEventType, value: A, key: String) = conn.publishEvent(typ, value, key)
  final override def declareEventExchange(klass: Class[_]) = conn.declareEventExchange(klass)

  // TODO: clone parent client settings?
  final override def login(authToken: String) = conn.login(authToken)
  final override def login(userName: String, password: String) = conn.login(userName, password)
  final override def login(userSettings: UserSettings) = conn.login(userSettings)

  final override def addRpcProvider(info: ServiceProviderInfo) = conn.addRpcProvider(info)
  final override def getRpcInterface[A](klass: Class[A]) = conn.getRpcInterface(klass, this)

  final override def addServiceInfo[A](info: ServiceTypeInformation[A, _]) = conn.addServiceInfo(info)
  final override def getServiceInfo[A](klass: Class[A]) = conn.getServiceInfo(klass)

  final override def addServicesList(servicesList: ServicesList) = conn.addServicesList(servicesList)

  final override def disconnect() = conn.disconnect()

  final override def logout() = conn.logout(this)
  final override def logout(authToken: String) = conn.logout(authToken)
  final override def logout(client: Client) = conn.logout(client)
}