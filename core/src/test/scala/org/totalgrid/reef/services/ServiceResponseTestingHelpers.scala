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

import org.totalgrid.reef.proto.Envelope.Status

import org.totalgrid.reef.messaging.{ AMQPProtoFactory }
import org.totalgrid.reef.util.BlockingQueue
import org.totalgrid.reef.protoapi.{ RequestEnv, ServiceHandlerHeaders, ProtoServiceTypes, StatusCodes }
import ProtoServiceTypes.{ Response, Event }
import org.totalgrid.reef.util.SyncVar

import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders

object ServiceResponseTestingHelpers extends ShouldMatchers {
  implicit def checkResponse[T](resp: Response[T]): List[T] = {
    StatusCodes.isSuccess(resp.status) should equal(true)
    resp.result
  }

  def one[T](status: Status, resp: Response[T]): T = {
    resp.status should equal(status)
    many(1, resp.result).head
  }

  def one[T](list: List[T]): T = many(1, list).head

  def none[T](list: List[T]) = many(0, list)

  def many[T](len: Int, list: List[T]): List[T] = {
    if (len != list.size) throw new Exception("list wrong size: " + list.size + " instead of: " + len + "\n" + list)
    list
  }

  def some[T](list: List[T]): List[T] = list

  def getEventQueue[T <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => T): (BlockingQueue[T], RequestEnv) = {

    val updates = new BlockingQueue[T]
    val env = getSubscriptionQueue(amqp, convert, { (evt: Event[T]) => updates.push(evt.result) })

    (updates, env)
  }

  def getEventQueueWithCode[T <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => T): (BlockingQueue[Event[T]], RequestEnv) = {
    val updates = new BlockingQueue[Event[T]]

    val env = getSubscriptionQueue(amqp, convert, { (evt: Event[T]) => updates.push(evt) })

    (updates, env)
  }

  def getSubscriptionQueue[T <: Any](amqp: AMQPProtoFactory, convert: Array[Byte] => T, func: Event[T] => Unit) = {

    val eventQueueName = new SyncVar("")
    val pointSource = amqp.getEventQueue[T](convert, func, { q => eventQueueName.update(q) })

    // wait for the queue name to get populated (actor startup delay)
    eventQueueName.waitWhile("")

    val env = new RequestEnv
    env.setSubscribeQueue(eventQueueName.current)

    env
  }
}