/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services

import org.scalatest.matchers.ShouldMatchers

import org.totalgrid.reef.protoapi.Envelope.Status

import org.totalgrid.reef.messaging.{ AMQPProtoFactory }
import org.totalgrid.reef.util.BlockingQueue

import org.totalgrid.reef.util.SyncVar

import org.totalgrid.reef.protoapi._
import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders
import ServiceTypes.{ Response, Event }

object ServiceResponseTestingHelpers extends ShouldMatchers {
  implicit def checkResponse[A](resp: Response[A]): List[A] = {
    StatusCodes.isSuccess(resp.status) should equal(true)
    resp.result
  }

  def one[A](status: Status, resp: Response[A]): A = {
    resp.status should equal(status)
    many(1, resp.result).head
  }

  def one[A](list: List[A]): A = many(1, list).head

  def none[A](list: List[A]) = many(0, list)

  def many[A](len: Int, list: List[A]): List[A] = {
    if (len != list.size) throw new Exception("list wrong size: " + list.size + " instead of: " + len + "\n" + list)
    list
  }

  def some[A](list: List[A]): List[A] = list

  def getEventQueue[A <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => A): (BlockingQueue[A], RequestEnv) = {

    val updates = new BlockingQueue[A]
    val env = getSubscriptionQueue(amqp, convert, { (evt: Event[A]) => updates.push(evt.result) })

    (updates, env)
  }

  def getEventQueueWithCode[A <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => A): (BlockingQueue[Event[A]], RequestEnv) = {
    val updates = new BlockingQueue[Event[A]]

    val env = getSubscriptionQueue(amqp, convert, { (evt: Event[A]) => updates.push(evt) })

    (updates, env)
  }

  def getSubscriptionQueue[A <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => A, func: Event[A] => Unit) = {

    val eventQueueName = new SyncVar("")
    val pointSource = amqp.getEventQueue[A](convert, func, { q => eventQueueName.update(q) })

    // wait for the queue name to get populated (actor startup delay)
    eventQueueName.waitWhile("")

    val env = new RequestEnv
    env.setSubscribeQueue(eventQueueName.current)

    env
  }
}