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

import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.{ Promise, RequestHeaders }
import org.totalgrid.reef.client.operations.{ RestOperations, Response }
import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.operations.impl._
import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s.Strand

trait OperationsBuilders {
  def buildRestOperations: RestOperations with OptionallyBatchedRestOperations
  def buildBatchOperations(rest: RestOperations): BatchRestOperations
}

abstract class OperationsBuildersImpl(val notifier: RequestListenerNotifier, val registry: ServiceRegistryLookup, val strand: Strand) extends ClientTiedOperationsBuilders

// TODO: cut the gordian knot here by just making default impls take clientimpl
trait ClientTiedOperationsBuilders extends OperationsBuilders {
  def issueRequest[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]]
  def notifier: RequestListenerNotifier
  def registry: ServiceRegistryLookup
  def strand: Strand

  def buildRestOperations: RestOperations with OptionallyBatchedRestOperations = new ClientTiedRestOperations
  def buildBatchOperations(rest: RestOperations): BatchRestOperations = new ClientTiedBatchRestOperations(rest)

  private class ClientTiedRestOperations extends RestOperations with DerivedRestOperations with OptionallyBatchedRestOperations {
    protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {
      issueRequest(verb, payload, headers)
    }

    def batched: Option[BatchRestOperations] = None
  }

  private class ClientTiedBatchRestOperations(protected val ops: RestOperations) extends BatchRestOperationsImpl {
    protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _] = registry.getServiceTypeInformation(klass)
    protected def futureSource[A](onAwait: Option[() => Unit]) = FuturePromise.openWithAwaitNotifier[A](strand, onAwait)
    protected def notifyListeners[A](verb: Envelope.Verb, payload: A, promise: Promise[Response[A]]) {
      notifier.notifyListeners(verb, payload, promise)
    }
  }
}
