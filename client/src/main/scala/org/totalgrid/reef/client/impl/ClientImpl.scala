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
import net.agileautomata.executor4s.{ Executor, Strand }
import operations._
import operations.impl._
import proto.Envelope.Verb
import registration.Service
import types.TypeDescriptor

class ClientImpl(conn: ConnectionImpl, strand: Strand) extends Client with ClientHeaders with SubscriptionListeners {

  def logout() {
    conn.logout(getHeaders.getAuthToken)
  }

  def spawn(): Client = conn.createClient(getHeaders.getAuthToken)

  def getServiceRegistry: ServiceRegistry = conn.getServiceRegistry

  def getService[A](klass: Class[A]): A = conn.Registry.buildServiceInterface(klass, this)

  object Binding extends BindOperations {
    def subscribe[T](descriptor: TypeDescriptor[T]): Subscription[T] = {
      notifySubscriptionCreated {
        conn.subscribe(descriptor, strand)
      }
    }
    def lateBindService[T](service: Service, descriptor: TypeDescriptor[T]): SubscriptionBinding = {
      notifySubscriptionCreated {
        conn.lateBindService(service, descriptor, strand)
      }
    }
  }

  object OpsBuilders extends OperationsBuildersImpl(RequestListeners, conn.Registry, strand) {
    def issueRequest[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {
      val usedHeaders = headers.map(getHeaders.merge(_)).getOrElse(getHeaders)
      val promise = conn.Sender.request(verb, payload, usedHeaders, strand)
      notifier.notifyListeners(verb, payload, promise)
      promise
    }
  }

  object BatchModeManager extends BatchingImpl(OpsBuilders)

  def getBatching: Batching = BatchModeManager

  def getServiceOperations: ServiceOperations = {
    val restOperations = BatchModeManager.getOps
    def createSingleOpsBatch() = {
      OpsBuilders.buildBatchOperations(restOperations)
    }
    new DefaultServiceOperations(restOperations, Binding, createSingleOpsBatch _, strand)
  }

  object RequestListeners extends RequestListenerManagerImpl

  def getRequestListenerManager: RequestListenerManager = RequestListeners

  object Internal extends ClientInternal {
    def getExecutor: Executor = strand
  }

  def getInternal: ClientInternal = Internal
}
