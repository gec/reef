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
package org.totalgrid.reef.clientapi.sapi.client.rpc.framework

import org.totalgrid.reef.clientapi.sapi.client.rest.{ BatchOperations, Client, AnnotatedOperations }
import org.totalgrid.reef.clientapi.sapi.client.rest.impl.{ BatchServiceRestOperations, DefaultAnnotatedOperations }
import org.totalgrid.reef.clientapi.{ SubscriptionCreator, SubscriptionCreationListener }
import org.totalgrid.reef.clientapi.exceptions.BadRequestException
import org.totalgrid.reef.clientapi.sapi.client._
import net.agileautomata.executor4s.{ TimeInterval, Executor }

trait HasAnnotatedOperations {
  protected def ops: AnnotatedOperations
  protected def client: Client
}

/**
 * client operations are all of the common client operations that we want to be able to include in all
 * ApiBase consumers.
 */
trait ClientOperations extends SubscriptionCreator with RequestSpyManager with HasHeaders with Executor with BatchOperations

abstract class ApiBase(protected val client: Client) extends HasAnnotatedOperations with ClientOperations {

  private var currentOpsMode = new DefaultAnnotatedOperations(client, client)
  private var flushableOps = Option.empty[BatchServiceRestOperations[_]]
  override def ops = currentOpsMode

  def startBatchRequests() {
    flushableOps = Some(new BatchServiceRestOperations(client))
    currentOpsMode = new DefaultAnnotatedOperations(flushableOps.get, client)
  }
  def stopBatchRequests() {
    flushableOps = None
    currentOpsMode = new DefaultAnnotatedOperations(client, client)
  }
  def flushBatchRequests() = {
    flushableOps match {
      case None => throw new BadRequestException("No batch requests configured")
      case Some(op) => op.flush()
    }
  }

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.addSubscriptionCreationListener(listener)
  override def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.removeSubscriptionCreationListener(listener)

  override def addRequestSpy(listener: RequestSpy): Unit = client.addRequestSpy(listener)
  override def removeRequestSpy(listener: RequestSpy): Unit = client.removeRequestSpy(listener)

  override def getHeaders = client.getHeaders
  override def setHeaders(headers: BasicRequestHeaders) = client.setHeaders(headers)
  override def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders) = client.modifyHeaders(modify)

  def attempt[A](fun: => A) = client.attempt(fun)
  def execute(fun: => Unit) = client.execute(fun)
  def schedule(interval: TimeInterval)(fun: => Unit) = client.schedule(interval)(fun)
  def scheduleWithFixedOffset(initial: TimeInterval, offset: TimeInterval)(fun: => Unit) =
    client.scheduleWithFixedOffset(initial, offset)(fun)
}

