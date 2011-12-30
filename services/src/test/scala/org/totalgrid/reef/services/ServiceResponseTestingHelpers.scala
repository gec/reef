/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services

import org.totalgrid.reef.test.BlockingQueue

import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Event }
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }

object ServiceResponseTestingHelpers {

  private def makeUuid(str: String) = ReefUUID.newBuilder.setValue(str).build
  implicit def makeUuidFromString(str: String): ReefUUID = makeUuid(str)

  private def makeId(str: String) = ReefID.newBuilder.setValue(str).build
  implicit def makeIdFromString(str: String): ReefID = makeId(str)

  def getEventQueue[A <: Any](amqp: Client, descriptor: TypeDescriptor[A]): (BlockingQueue[A], BasicRequestHeaders) = {

    val updates = BlockingQueue.empty[A]
    val env = getSubscriptionQueue(amqp, descriptor, { (evt: Event[A]) => updates.push(evt.value) })

    (updates, env)
  }

  def getEventQueueWithCode[A <: Any](amqp: Client, descriptor: TypeDescriptor[A]): (BlockingQueue[Event[A]], BasicRequestHeaders) = {
    val updates = BlockingQueue.empty[Event[A]]

    val env = getSubscriptionQueue(amqp, descriptor, { (evt: Event[A]) => updates.push(evt) })

    (updates, env)
  }

  def getSubscriptionQueue[A <: Any](amqp: Client, descriptor: TypeDescriptor[A], func: Event[A] => Unit) = {

    val sub = amqp.subscribe(descriptor)

    sub.start(func)

    BasicRequestHeaders.empty.setSubscribeQueue(sub.id)

  }
}