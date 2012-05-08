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
package org.totalgrid.reef.client.impl

import org.totalgrid.reef.client._
import registration.{ Service, ServiceRegistration }
import settings.UserSettings
import org.totalgrid.reef.broker.{ BrokerConnectionListener, BrokerConnection }
import net.agileautomata.executor4s.{ Strand, Executor }
import types.TypeDescriptor

class ConnectionImpl(broker: BrokerConnection, executor: Executor, timeoutMs: Long)
    extends Connection with ConnectionListening {

  private val requests = RequestManager(broker, executor, timeoutMs)

  broker.addListener(BrokerConnection)

  object BrokerConnection extends BrokerConnectionListener {
    def onDisconnect(expected: Boolean) {
      requests.close()
      notifyListenersOfClose(expected)
    }
  }

  def disconnect() {
    // TODO: unstub
  }

  object Sender extends RequestSenderImpl(requests, Registry)

  private val me = this

  object Login extends ClientLogin(Sender, executor) {
    def createClient(authToken: String, strand: Strand): ClientImpl = {
      val cl = new ClientImpl(me, strand)
      cl.getHeaders.setAuthToken(authToken)
      cl
    }
  }

  def login(userSettings: UserSettings): Client = {
    Login.login(userSettings.getUserName, userSettings.getUserPassword).await()
  }

  def createClient(authToken: String): Client = {
    Login.createClient(authToken, Strand(executor))
  }

  def logout(authToken: String) {
    Login.logout(authToken, Strand(executor)).await()
  }

  object Registration extends ServiceRegistrationImpl(broker, Registry, executor)

  def getServiceRegistration: ServiceRegistration = Registration

  def subscribe[A](descriptor: TypeDescriptor[A], exe: Executor) = {
    new DefaultSubscription[A](broker.listen(), exe, descriptor.deserialize)
  }

  def lateBindService[A](service: Service, descriptor: TypeDescriptor[A], exe: Executor): SubscriptionBinding = {
    val sub = new DefaultServiceBinding[A](broker, broker.listen(), exe)
    sub.start(service)
    sub
  }

  object Internal extends ConnectionInternal {
    def getExecutor: Executor = executor
  }

  def getInternal: ConnectionInternal = Internal

  object Registry extends ServiceRegistryImpl

  def getServiceRegistry: ServiceRegistry = Registry

  def addServicesList(servicesList: ServicesList) {
    Registry.addServicesList(servicesList)
  }
}
