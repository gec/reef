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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.{ Batching, Promise, RequestHeaders, Client }
import ScalaServiceOperations._
import org.totalgrid.reef.client.operations.impl.FuturePromise
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.operations.{ RequestListener, RequestListenerManager, ServiceOperations }

abstract class ServiceOperationsProvider(client: Client)
    extends UsesServiceOperations with UsesServiceRegistry with HasHeaders with HasBatching with RequestListenerManager {

  protected def ops: ServiceOperations = client.getServiceOperations
  def batching: Batching = client.getBatching

  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _] = client.getInternal.getServiceRegistry.getServiceInfo(klass)

  def getHeaders() = client.getHeaders

  def setHeaders(hdrs: RequestHeaders) {
    client.setHeaders(hdrs)
  }

  def collate[A](promises: List[Promise[A]]): Promise[List[A]] = {
    gather(client.getInternal.getExecutor, promises)
  }

  def batchGets[A](gets: List[A]): Promise[List[A]] = {
    ops.batchOperation("Error during batched 'get' request") { session =>
      collate(gets.map(session.get(_).map(_.one)))
    }
  }

  protected def gather[A](exe: Executor, promises: List[Promise[A]]): Promise[List[A]] = {

    val f = FuturePromise.open[List[A]](exe)
    val size = promises.size
    val map = collection.mutable.Map.empty[Int, Promise[A]]

    def gather(i: Int)(prom: Promise[A]) = map.synchronized {
      map.put(i, prom)
      if (map.size == size) {
        val all = promises.indices.map(map(_))
        try {
          f.setSuccess(all.map(_.await()).toList)
        } catch {
          case ex: ReefServiceException => f.setFailure(ex)
        }
      }
    }

    if (promises.isEmpty) f.setSuccess(Nil)
    else promises.zipWithIndex.foreach { case (f, i) => f.listenFor(gather(i)) }
    f
  }

  def addRequestListener(listener: RequestListener) {
    client.getRequestListenerManager.addRequestListener(listener)
  }

  def removeRequestListener(listener: RequestListener) {
    client.getRequestListenerManager.removeRequestListener(listener)
  }
}
