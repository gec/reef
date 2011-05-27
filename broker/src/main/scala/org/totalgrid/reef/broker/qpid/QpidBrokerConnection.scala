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
package org.totalgrid.reef.broker.qpid

import org.apache.qpid.transport.{ Connection }
import org.apache.qpid.transport.{ ConnectionListener, ConnectionException }

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.broker._
import scala.{ Some, Option => ScalaOption }

class QpidBrokerConnection(config: BrokerConnectionInfo) extends BrokerConnection with Logging {

  case class ConnectionRecord(connection: Connection, listener: Listener)

  private var connection: ScalaOption[ConnectionRecord] = None

  class Listener(qpid: QpidBrokerConnection) extends ConnectionListener {

    private var valid = true

    def invalidate() = valid = false

    def closed(conn: Connection) = if (valid) qpid.onClosed()

    def opened(conn: Connection) = if (valid) qpid.onOpened(conn)

    def exception(conn: Connection, ex: ConnectionException) = if (valid) qpid.onException(conn, ex)

  }

  override def toString() = config.toString

  final override def connect(): Boolean = connection match {
    case Some(c) => true
    case None =>
      val conn = new Connection
      val listener = new Listener(this)
      conn.addConnectionListener(listener)
      logger.info("Connecting to " + config)
      try {
        conn.connect(config.host, config.port, config.virtualHost, config.user, config.password, false)
        connection = Some(ConnectionRecord(conn, listener))
        this.setOpen()
        true
      } catch {
        case ex: Exception =>
          logger.error(ex.getMessage, ex)
          false
      }
  }

  final override def disconnect(): Boolean = connection match {
    case Some(ConnectionRecord(c, l)) =>
      l.invalidate()
      c.close()
      cleanupAfterClose(true)
      true
    case None =>
      true
  }

  private def unlinkChannels() = {
    channels.foreach(channel =>
      try {
        channel.stop()
      } catch {
        case e: Exception => logger.warn("Qpid unlink error: " + e.getMessage)
      })
    channels = Nil
  }

  def onClosed() {
    logger.info("Qpid connection unexpectedly closed")
    cleanupAfterClose(false)
  }

  private def cleanupAfterClose(expected: Boolean) = {
    connection = None
    this.setClosed(expected)
    unlinkChannels()
  }

  def onOpened(conn: Connection) {
    logger.info("Qpid Connection opened")
  }

  def onException(conn: Connection, ex: ConnectionException) {
    logger.error("Connection Exception: ", ex)
  }

  /* -- End Qpid Connection Listener -- */

  // TODO - Looks like this list of channels never shrinks as sessions die? - JAC
  private var channels = List.empty[QpidBrokerChannel]

  final override def newChannel(): BrokerChannel = connection match {
    case Some(ConnectionRecord(c, _)) =>
      val channel = new QpidBrokerChannel(c.createSession(0))
      channels = channel :: channels
      channel
    case None =>
      throw new Exception("Connection is closed, cannot create channel")
  }

}
