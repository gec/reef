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
import sapi.client.BasicRequestHeaders
import sapi.types.BuiltInDescriptors
import settings.UserSettings
import org.totalgrid.reef.broker.{ BrokerConnectionListener, BrokerConnection }
import net.agileautomata.executor4s.{ Strand, Executor }
import types.TypeDescriptor
import com.typesafe.scalalogging.slf4j.Logging

class ConnectionImpl(broker: BrokerConnection, executor: Executor, timeoutMs: Long)
    extends Connection with ConnectionListening with Logging { self =>

  // Service registry component and public interface.
  // registry is used by ClientImpl
  val registry = new ServiceRegistryImpl

  registry.addServiceTypeInformation(BuiltInDescriptors.authRequestServiceInfo)
  registry.addServiceTypeInformation(BuiltInDescriptors.batchServiceRequestServiceInfo)

  // ServiceRegistration component
  private val registration = new ServiceRegistrationImpl(broker, registry, executor)

  // Request components - request manager handles correlation/resources,
  // request sender encapsulates payload class lookup
  private val requestManager = RequestManager(broker, executor, timeoutMs)
  val requestSender = new RequestSenderImpl(requestManager, registry)

  // Login component encapsulates login service requests
  private val login = new ClientLogin(requestSender, executor) {
    def createClient(headers: RequestHeaders, strand: Strand): ClientImpl = {
      val cl = new ClientImpl(self, strand)
      cl.setHeaders(headers)
      cl
    }
  }

  // Callback object for broker-caused disconnections
  private val brokerListener = new BrokerConnectionListener {
    def onDisconnect(expected: Boolean) {
      logger.info("connection disconnected: " + expected)
      handleDisconnect(expected)
    }
  }

  // Register callback with broker
  broker.addListener(brokerListener)

  // Disconnection logic, two ways to disconnect: by listening to the broker or
  // by an explicit disconnect() call
  private def handleDisconnect(expected: Boolean) {
    broker.removeListener(brokerListener)
    requestManager.close()
    notifyListenersOfClose(expected)
  }

  def disconnect() {
    val currentlyConnected = broker.isConnected()
    logger.info("disconnect called, connected: " + currentlyConnected)
    requestManager.cancelSubscription()
    if (currentlyConnected) {
      handleDisconnect(true)
      broker.disconnect()
    }
  }

  // Login public interface
  def login(userSettings: UserSettings): Client = {
    login.login(userSettings.getUserName, userSettings.getUserPassword).await()
  }

  def createClient(authToken: String): Client = {
    login.createClient(BasicRequestHeaders.fromAuth(authToken), Strand(executor))
  }

  def logout(authToken: String) {
    login.logout(authToken, Strand(executor)).await()
  }

  // Used by ClientImpl to copy auth and relevant settings
  def copyClient(headers: RequestHeaders): Client = {
    login.createClient(headers, Strand(executor))
  }

  // ServiceRegistration public interface
  def getServiceRegistration: ServiceRegistration = registration

  // Subscription internal interfaces used by ClientImpl
  def subscribe[A](descriptor: TypeDescriptor[A], exe: Executor) = {
    new DefaultSubscription[A](broker.listen(), exe, descriptor.deserialize)
  }

  def lateBindService[A](service: Service, descriptor: TypeDescriptor[A], exe: Executor): SubscriptionBinding = {
    val sub = new DefaultServiceBinding[A](broker, broker.listen(), exe)
    sub.start(service)
    sub
  }

  // Service registry public interface
  def getServiceRegistry: ServiceRegistry = registry

  def addServicesList(servicesList: ServicesList) {
    registry.addServicesList(servicesList)
  }

  // ConnectionInternal exposes executor
  private val internal = new ConnectionInternal {
    def getExecutor: Executor = executor
  }

  def getInternal: ConnectionInternal = internal
}
