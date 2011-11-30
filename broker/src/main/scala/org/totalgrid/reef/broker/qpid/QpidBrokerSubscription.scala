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
package org.totalgrid.reef.broker.qpid

import org.totalgrid.reef.broker._
import org.apache.qpid.transport._
import scala.{ Option => ScalaOption }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.exceptions.ServiceIOException

final class QpidBrokerSubscription(session: Session, queue: String, connection: QpidBrokerConnection) extends BrokerSubscription with Logging {

  class Listener(consumer: BrokerMessageConsumer) extends SessionListener {

    override def closed(s: Session) = logger.info("Qpid session closed")
    override def exception(s: Session, e: SessionException) = logger.error("Qpid session exception", e)
    override def opened(s: Session) = logger.info("Qpid session opened")
    override def resumed(s: Session) = logger.info("Qpid session resumed")

    override def message(s: Session, msg: MessageTransfer) {
      val replyTo = ScalaOption(msg.getHeader.get(classOf[MessageProperties]).getReplyTo)
      val dest = replyTo.map(r => new BrokerDestination(r.getExchange, r.getRoutingKey))
      try {
        consumer.onMessage(BrokerMessage(msg.getBodyBytes, dest))
      } catch {
        case ex: Exception =>
          logger.error("Exception thrown during subscription event processing.", ex)
      }
      s.processed(msg)

    }
  }

  def start(consumer: BrokerMessageConsumer): BrokerSubscription = {
    if (!connection.isConnected()) throw new ServiceIOException("Connection closed")
    // TODO: shoud we remove session listener on close?
    session.setSessionListener(new Listener(consumer))
    if (!session.isClosing) QpidChannelOperations.subscribe(session, queue)
    this
  }

  def getQueue = queue
  def close() {
    QpidChannelOperations.close(session)
    connection.detachSession(session)
  }

}