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

import scala.{ Option => ScalaOption }

import org.apache.qpid.transport._

import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.broker._

final class QpidWorkerChannel(val session: Session, connection: QpidBrokerConnection, ttlMilliseconds: Int) extends SessionListener with Logging {

  def isOpen = !session.isClosing

  session.setSessionListener(this)
  session.setAutoSync(true)

  def closed(s: Session) = logger.info("Qpid session closed")
  def exception(s: Session, e: SessionException) = logger.error("Qpid session exception", e)
  def opened(s: Session) = logger.info("Qpid session closed")
  def resumed(s: Session) = logger.info("Qpid session resumed")
  def message(s: Session, msg: MessageTransfer): Unit = logger.error("Unexpected msg on worker channel: " + msg)

  /* -- Operations -- */

  def declareQueue(queueNameTemplate: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String =
    QpidChannelOperations.declareQueue(session, queueNameTemplate, autoDelete, exclusive)

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit =
    QpidChannelOperations.declareExchange(session, exchange, exchangeType)

  def bindQueue(queue: String, exchange: String, key: String, unbindFirst: Boolean): Unit =
    QpidChannelOperations.bindQueue(session, queue, exchange, key, unbindFirst)

  def unbindQueue(queue: String, exchange: String, key: String): Unit =
    QpidChannelOperations.unbindQueue(session, queue, exchange, key)

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: ScalaOption[BrokerDestination]) =
    QpidChannelOperations.publish(session, exchange, key, b, replyTo, ttlMilliseconds)

  def close() {
    QpidChannelOperations.close(session)
    connection.detachSession(session)
  }
}