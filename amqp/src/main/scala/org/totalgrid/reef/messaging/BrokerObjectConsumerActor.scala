/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging

import scala.actors.Actor
import scala.actors.Actor._

import scala.collection.immutable.Queue
import org.totalgrid.reef.reactor.Reactable

trait BrokerObjectConsumer {
  type BrokerApplicable = (BrokerChannel) => _

  def sendTo(b: BrokerApplicable): Unit
}

/**
 * This actor simplifies objects that need to handle the broker coming up and down and
 * queuing operations until the broker is online.
 */
class BrokerObjectConsumerActor(reactor: Reactable) extends BrokerObjectConsumer with ChannelObserver {

  private var channel: Option[BrokerChannel] = None
  private var queued: Queue[BrokerApplicable] = Queue.empty

  def sendTo(b: BrokerApplicable): Unit = reactor.execute {
    channel match {
      case Some(c) =>
        b(c)
      case None =>
        queued = queued.enqueue(b)
    }
  }

  def online(b: BrokerChannel) = reactor.execute {
    onConnect(b)
    channel = Some(b)
    queued.foreach(_(b))
    queued = Queue.empty
  }
  def offline() = reactor.execute {
    channel = None
  }

  def onConnect(b: BrokerChannel) = {}

}