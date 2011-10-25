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
package org.totalgrid.reef.api.sapi.client.rpc.framework

import org.totalgrid.reef.api.sapi.client.rest.{ Client, AnnotatedOperations }
import org.totalgrid.reef.api.sapi.rest.impl.DefaultAnnotatedOperations
import org.totalgrid.reef.api.japi.client.{ SubscriptionCreationListener, SubscriptionCreator }
import org.totalgrid.reef.api.sapi.client.{ RequestSpy, RequestSpyManager, BasicRequestHeaders, HasHeaders }
import net.agileautomata.executor4s.{ Timer, TimeInterval, Executor }

trait HasAnnotatedOperations {
  protected def ops: AnnotatedOperations
}

/**
 * client operations are all of the common client operations that we want to be able to include in all
 * ApiBase consumers.
 */
trait ClientOperations extends SubscriptionCreator with RequestSpyManager with HasHeaders with Executor

abstract class ApiBase(client: Client) extends HasAnnotatedOperations with ClientOperations {

  override val ops = new DefaultAnnotatedOperations(client)

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = ops.addSubscriptionCreationListener(listener)

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

