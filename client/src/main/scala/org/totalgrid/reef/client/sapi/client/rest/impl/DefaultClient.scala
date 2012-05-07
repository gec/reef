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
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.client.registration.Service

import org.totalgrid.reef.client.types.{ ServiceTypeInformation, TypeDescriptor }
import org.totalgrid.reef.client.settings.UserSettings
import org.totalgrid.reef.client.javaimpl.ClientWrapper
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.{ Promise => JPromise }
import org.totalgrid.reef.client.{ ServicesList, ServiceProviderInfo, Routable }
import org.totalgrid.reef.client.operations.{ RequestListenerManager, RequestListener, Response => JResponse }
import org.totalgrid.reef.client.sapi.client.{ SubscriptionCreatorManager, DefaultHeaders, BasicRequestHeaders }

class DefaultClient(conn: DefaultConnection, strand: Strand)
    extends DefaultHeaders with SubscriptionCreatorManager with ExecutorDelegate with SharedServiceRegistry {

  protected def executor = strand

  private var listeners = Set.empty[RequestListener]

  def listenerManager: RequestListenerManager = new RequestListenerManager {
    def addRequestListener(listener: RequestListener) {
      listeners += listener
    }

    def removeRequestListener(listener: RequestListener) {
      listeners -= listener
    }
  }

  def notifyListeners[A](verb: Envelope.Verb, payload: A, promise: JPromise[JResponse[A]]) {
    listeners.foreach(_.onRequest(verb, payload, promise))
  }

  def requestJava[A](verb: Envelope.Verb, payload: A, headers: Option[BasicRequestHeaders]): JPromise[JResponse[A]] = {
    val usedHeaders = headers.map(getHeaders.merge(_)).getOrElse(getHeaders)
    val promise = conn.requestJava(verb, payload, usedHeaders, strand)
    notifyListeners(verb, payload, promise)
    promise
  }

  // implement ClientBindOperations
  def subscribe[A](descriptor: TypeDescriptor[A]) = {
    notifySubscriptionCreated(conn.subscribe(descriptor, strand))
  }
  def lateBindService[A](service: Service, descriptor: TypeDescriptor[A]) = {
    notifySubscriptionCreated(conn.lateBindService(service, descriptor, strand))
  }

  // implement Bindable
  def subscribe[A](descriptor: TypeDescriptor[A], dispatcher: Executor) = {
    notifySubscriptionCreated(conn.subscribe(descriptor, dispatcher))
  }
  def bindService[A](service: Service, descriptor: TypeDescriptor[A], dispatcher: Executor, destination: Routable, competing: Boolean) = {
    notifySubscriptionCreated(conn.bindService(service, descriptor, dispatcher, destination, competing))
  }
  def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) {
    conn.bindQueueByClass(subQueue, key, klass)
  }
  def lateBindService[A](service: Service, descriptor: TypeDescriptor[A], dispatcher: Executor) =
    notifySubscriptionCreated(conn.lateBindService(service, descriptor, dispatcher))
  def bindServiceQueue[A](subQueue: String, key: String, klass: Class[A]) {
    conn.bindServiceQueue(subQueue, key, klass)
  }

  def publishEvent[A](typ: SubscriptionEventType, value: A, key: String) {
    conn.publishEvent(typ, value, key)
  }
  def declareEventExchange(klass: Class[_]) {
    conn.declareEventExchange(klass)
  }

  // TODO: clone parent client settings?
  def login(authToken: String) = conn.login(authToken)
  def login(userName: String, password: String) = conn.login(userName, password)
  def login(userSettings: UserSettings) = conn.login(userSettings)
  def spawn() = conn.login(getHeaders.getAuthToken)

  def addRpcProvider(info: ServiceProviderInfo) {
    conn.addRpcProvider(info)
  }
  def getRpcInterface[A](klass: Class[A]) = conn.getRpcInterface(klass, new ClientWrapper(this))

  def addServiceInfo[A](info: ServiceTypeInformation[A, _]) {
    conn.addServiceInfo(info)
  }
  def getServiceInfo[A](klass: Class[A]) = conn.getServiceInfo(klass)

  def addServicesList(servicesList: ServicesList) {
    conn.addServicesList(servicesList)
  }

  def disconnect() = conn.disconnect()

  def logout() = conn.logout(this)
  def logout(authToken: String) = conn.logout(authToken)
  def logout(client: DefaultClient) = conn.logout(client)
}