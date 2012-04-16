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
package org.totalgrid.reef.client.sapi.client.rpc.framework

import org.totalgrid.reef.client.sapi.client.rest._
import org.totalgrid.reef.client.sapi.client.rest.impl.{ BatchServiceRestOperations, DefaultAnnotatedOperations }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.sapi.client._
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.{ RequestHeaders, SubscriptionCreator, SubscriptionCreationListener, Client }

trait HasAnnotatedOperations {
  protected def ops: AnnotatedOperations
  /*
  protected def client: Client
  protected def restOps: RestOperations
  protected def spyHooks: RestOperations
  protected def serviceRegistry: ServiceRegistry
  */
  protected def serviceRegistry: ServiceRegistry
  protected def exe: Executor
  protected def buildBatchRestOps: BatchServiceRestOperations

  /**
   * do a set of operations as a single batch request (not for use for multi-step requests)!
   */
  def batch[A](fun: (RestOperations) => A): A = {
    val batch = buildBatchRestOps
    val result = fun(batch)
    batch.flush()
    result
  }

  def batchGets[A](gets: List[A]): Future[Result[List[A]]] = {
    // do all of queries in a single batch request (instead of N seperate queries which though should actually
    // be slightly faster it uses much much more server resources)
    val requests = batch { batchSession =>
      gets.map { g => batchSession.get(g).map(_.one) }
    }
    MultiRequestHelper.gatherResults(exe, requests)
  }
}

/**
 * client operations are all of the common client operations that we want to be able to include in all
 * ApiBase consumers.
 */
trait ClientOperations extends SubscriptionCreator with RequestSpyManager with HasHeaders with Executor with BatchOperations

abstract class ApiBase(protected val client: Client) extends HasAnnotatedOperations with ClientOperations with ExecutorDelegate {

  protected val restOps = client.getInternal.getOperations
  protected val exe = client.getInternal.getExecutor
  protected val spyHooks = client.getInternal.getRequestSpyHook
  protected val serviceRegistry = client.getInternal.getServiceRegistry

  protected def buildBatchRestOps: BatchServiceRestOperations = {
    new BatchServiceRestOperations(restOps, spyHooks, serviceRegistry, exe)
  }

  private var currentOpsMode = new DefaultAnnotatedOperations(restOps, exe)
  private var flushableOps = Option.empty[BatchServiceRestOperations]
  def ops = currentOpsMode

  def startBatchRequests() {
    // TODO: add clearBatchRequests and checking that batched operations have been flushed
    flushableOps = Some(new BatchServiceRestOperations(restOps, spyHooks, serviceRegistry, exe))
    currentOpsMode = new DefaultAnnotatedOperations(flushableOps.get, exe)
  }
  def stopBatchRequests() {
    flushableOps = None
    currentOpsMode = new DefaultAnnotatedOperations(restOps, exe)
  }
  def flushBatchRequests() = {
    flushableOps match {
      case None => throw new BadRequestException("No batch requests configured")
      case Some(op) => op.flush()
    }
  }
  def batchedFlushBatchRequests(size: Int) = {
    flushableOps match {
      case None => throw new BadRequestException("No batch requests configured")
      case Some(op) => op.batchedFlush(size)
    }
  }

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.addSubscriptionCreationListener(listener)
  override def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.removeSubscriptionCreationListener(listener)

  override def addRequestSpy(listener: RequestSpy) { spyHooks.addRequestSpy(listener) }
  override def removeRequestSpy(listener: RequestSpy) { spyHooks.removeRequestSpy(listener) }

  override def getHeaders = client.getInternal.getHeaders
  override def setHeaders(headers: BasicRequestHeaders) { client.getInternal.setHeaders(headers) }
  override def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders) = {
    client.getInternal.setHeaders(modify(client.getInternal.getHeaders))
  }

  // for ExecutorDelegate
  protected def executor = exe

}

