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
package org.totalgrid.reef.messaging.qpid

import scala.{ Option => ScalaOption }

import org.apache.qpid.AMQException
import org.apache.qpid.transport.{ Session, SessionListener, SessionException }
import org.apache.qpid.transport.{ DeliveryProperties, MessageProperties, Header, ReplyTo, MessageTransfer }
import org.apache.qpid.transport.{ Option, MessageAcceptMode, MessageAcquireMode, MessageCreditUnit }

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.messaging.{ BrokerChannel, MessageConsumer, Destination }
import org.totalgrid.reef.api.ServiceIOException

class QpidBrokerInterface(session: Session) extends SessionListener with BrokerChannel with Logging {
  var messageConsumer: ScalaOption[MessageConsumer] = None

  session.setSessionListener(this)

  def closed(s: Session) {
    info("Qpid session closed")
  }

  def exception(s: Session, e: SessionException) {
    warn(e)
    // TODO: on exception we need to throw away session, it is closed
  }

  def opened(s: Session) {}

  def resumed(s: Session) {}

  case class RecievedData(data: Array[Byte], reply: ScalaOption[Destination])

  def message(s: Session, msg: MessageTransfer) = {
    val replyTo = msg.getHeader.get(classOf[MessageProperties]).getReplyTo
    val dest = if (replyTo == null) None else Some(new Destination(replyTo.getExchange, replyTo.getRoutingKey))
    messageConsumer.foreach { mc => mc.receive(msg.getBodyBytes, dest) }
  }

  /* -- Implement BrokerChannel -- */

  def close() = {
    // qpid just does a 60 second timeout if close is called more than once
    if (!session.isClosing()) {
      debug("closing session")
      session.close()
    }
  }

  def declareQueue(queueNameTemplate: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    val starIndex = queueNameTemplate.indexOf("*")
    val queue = if (starIndex != -1) queueNameTemplate.patch(starIndex, session.getName.toString, 1) else queueNameTemplate
    var l = List.empty[Option]
    if (autoDelete) l ::= Option.AUTO_DELETE
    if (exclusive) l ::= Option.EXCLUSIVE
    session.queueDeclare(queue, null, null, l: _*)
    debug("Declared Queue: " + queue)
    queue //return the unique queue name
  }

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    if (!exchange.startsWith("amq.")) {
      // Qpid quietly kills your session if you try to declare a built in queue, reevaluate if we switch to rabbit
      session.exchangeDeclare(exchange, exchangeType, null, null)
      debug("Declared Exchange: " + exchange)
    }
  }

  def bindQueue(queue: String, exchange: String, key: String, unbindFirst: Boolean): Unit = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    if (unbindFirst) {
      unbindQueue(queue, exchange, key)
    }
    session.exchangeBind(queue, exchange, key, null)
    debug("Bound: " + queue + " to " + exchange + " key: " + key)
  }

  def unbindQueue(queue: String, exchange: String, key: String): Unit = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    session.exchangeUnbind(queue, exchange, key)
    debug("Removed Binding: " + queue + " to " + exchange + " key: " + key)
  }

  def listen(queue: String, mc: MessageConsumer) = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    debug("Listening: " + queue + " to " + mc)
    messageConsumer = Some(mc)
    session.messageSubscribe(queue, queue, MessageAcceptMode.NONE, MessageAcquireMode.PRE_ACQUIRED, null, 0, null)
    session.messageFlow(queue, MessageCreditUnit.BYTE, Session.UNLIMITED_CREDIT)
    session.messageFlow(queue, MessageCreditUnit.MESSAGE, Session.UNLIMITED_CREDIT)
  }

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: ScalaOption[Destination]) = {

    if (session.isClosing()) throw new ServiceIOException("Session unexpectedly closing/closed")

    val dev_props = new DeliveryProperties
    val msg_props = new MessageProperties
    dev_props.setRoutingKey(key)

    replyTo match { //optional parameters

      case Some(Destination(ex, replykey)) =>
        msg_props.setReplyTo(new ReplyTo(ex, replykey))
      case None =>
    }
    val hdr = new Header(dev_props, msg_props)
    session.messageTransfer(exchange, MessageAcceptMode.EXPLICIT, MessageAcquireMode.PRE_ACQUIRED, hdr, b)
  }

  def unlink() {
    messageConsumer = None
    session.setSessionListener(null)
  }

  def stop() {
    unlink()
    close
  }
}