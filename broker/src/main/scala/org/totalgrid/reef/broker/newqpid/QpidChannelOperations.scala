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
package org.totalgrid.reef.broker.newqpid

import org.apache.qpid.transport._
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.japi.ServiceIOException
import scala.{ Option => ScalaOption }
import org.totalgrid.reef.broker.newapi.BrokerDestination

class ChannelClosedException extends ServiceIOException("Session unexpectedly closing/closed")

/**
 * Helper methods for doing operations on qpid channels
 */
object QpidChannelOperations extends Logging {

  def subscribe(session: Session, queue: String): Unit = {
    if (session.isClosing) throw new ChannelClosedException
    session.messageSubscribe(queue, queue, MessageAcceptMode.NONE, MessageAcquireMode.PRE_ACQUIRED, null, 0, null)
    session.messageFlow(queue, MessageCreditUnit.BYTE, Session.UNLIMITED_CREDIT)
    session.messageFlow(queue, MessageCreditUnit.MESSAGE, Session.UNLIMITED_CREDIT)
    session.sync()
    logger.debug("subscribed to queue: " + queue)
  }

  def declareQueue(session: Session, queueNameTemplate: String, autoDelete: Boolean, exclusive: Boolean): String = {

    if (session.isClosing) throw new ChannelClosedException

    val starIndex = queueNameTemplate.indexOf("*")
    val queue = if (starIndex != -1) queueNameTemplate.patch(starIndex, session.getName.toString, 1) else queueNameTemplate
    var l = List.empty[Option]
    if (autoDelete) l ::= Option.AUTO_DELETE
    if (exclusive) l ::= Option.EXCLUSIVE
    session.queueDeclare(queue, null, null, l: _*)
    session.sync()
    logger.debug("Declared Queue: " + queue)
    queue //return the unique queue name
  }

  def declareExchange(session: Session, exchange: String, exchangeType: String): Unit = {

    if (session.isClosing) throw new ChannelClosedException

    if (!exchange.startsWith("amq.")) {
      // Qpid quietly kills your session if you try to declare a built in queue, reevaluate if we switch to rabbit
      if (exchange.trim.length < 1) throw new Exception("Bad exchange name: " + exchange)
      session.exchangeDeclare(exchange, exchangeType, null, null)
      session.sync()
      logger.debug("Declared Exchange: " + exchange)
    }

  }

  def bindQueue(session: Session, queue: String, exchange: String, key: String, unbindFirst: Boolean): Unit = {
    if (session.isClosing) throw new ChannelClosedException
    if (unbindFirst) unbindQueue(session, queue, exchange, key)
    session.exchangeBind(queue, exchange, key, null)
    session.sync()
    logger.debug("Bound " + describeBinding(queue, exchange, key))
  }

  def unbindQueue(session: Session, queue: String, exchange: String, key: String): Unit = {
    if (session.isClosing) throw new ChannelClosedException
    session.exchangeUnbind(queue, exchange, key)
    session.sync()
    logger.debug("Unbound " + describeBinding(queue, exchange, key))
  }

  def publish(session: Session, exchange: String, key: String, b: Array[Byte], replyTo: ScalaOption[BrokerDestination]) = {
    if (session.isClosing) throw new ChannelClosedException
    val dev_props = new DeliveryProperties
    val msg_props = new MessageProperties
    dev_props.setRoutingKey(key)
    replyTo.foreach(r => msg_props.setReplyTo(new ReplyTo(r.exchange, r.key)))
    val hdr = new Header(dev_props, msg_props)
    session.messageTransfer(exchange, MessageAcceptMode.NONE, MessageAcquireMode.PRE_ACQUIRED, hdr, b)
    session.sync()
  }

  def describeBinding(queue: String, exchange: String, key: String): String =
    "queue " + queue + " to exchange " + exchange + " w/ key " + key
}