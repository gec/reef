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
package org.totalgrid.reef.app

import com.weiglewilczek.slf4s.Logging
import net.agileautomata.executor4s._
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.clientapi.settings.AmqpSettings
import org.totalgrid.reef.util.{ Lifecycle, Cancelable => UCancelable }
import org.totalgrid.reef.clientapi.sapi.client.rest.ConnectionWatcher
import org.totalgrid.reef.clientapi.sapi.client.rest.impl.DefaultReconnectingFactory
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory

/**
 * handles the connection to an amqp broker, passing the valid and created Connection to the
 * applicationCreator function
 */
class ConnectionCloseManagerEx(amqpSettings: AmqpSettings, exe: Executor)
    extends ConnectionWatcher
    with Lifecycle
    with Logging {

  private val fac = new QpidBrokerConnectionFactory(amqpSettings)
  private val factory = new DefaultReconnectingFactory(fac, exe, 1000, 10000)
  factory.addConnectionWatcher(this)

  private var connection = Option.empty[BrokerConnection]
  private var consumers = Map.empty[ConnectionConsumer, Option[UCancelable]]
  private var delays = Map.empty[ConnectionConsumer, Cancelable]

  def addConsumer(generator: ConnectionConsumer): Unit = this.synchronized {
    if (consumers.get(generator).isDefined) throw new IllegalArgumentException("Consumer already added")
    consumers += generator -> None
    connection.foreach { conn =>
      doBrokerConnectionStarted(generator, conn)
    }
  }

  def removeConsumer(generator: ConnectionConsumer): Unit = this.synchronized {
    if (consumers.get(generator).isEmpty) throw new IllegalArgumentException("Unknown consumer")
    doBrokerConnectionLost(generator)
    consumers -= generator
  }

  def onConnectionOpened(conn: BrokerConnection) = this.synchronized {
    if (connection.isDefined) logger.error("Connection is already defined")
    connection = Some(conn)
    cancelDelays()
    consumers.keys.foreach { doBrokerConnectionStarted(_, conn) }
  }

  def onConnectionClosed(expected: Boolean) = this.synchronized {
    connection = None
    cancelDelays()
    if (!expected) {
      logger.warn("Connection to broker " + amqpSettings + " lost.")
      consumers.keys.foreach { doBrokerConnectionLost(_) }
    }
  }

  private def cancelDelays() {
    delays.values.foreach { _.cancel }
    delays = Map.empty[ConnectionConsumer, Cancelable]
  }

  private def doBrokerConnectionStarted(generator: ConnectionConsumer, conn: BrokerConnection): Unit = this.synchronized {
    consumers.get(generator).foreach { cancelable =>
      if (cancelable.isDefined) {
        logger.error("Still have an active application instance onConnectionOpened!")
        doBrokerConnectionLost(generator)
      }
    }
    val application = tryC("Couldn't start consumer successfully: ") {
      generator.newConnection(conn, exe)
    }
    consumers += generator -> application

    if (application.isEmpty) {
      delays += generator -> exe.schedule(5000.milliseconds) { connection.foreach(doBrokerConnectionStarted(generator, _)) }
    }
  }

  private def doBrokerConnectionLost(generator: ConnectionConsumer) = this.synchronized {
    tryC("Couldn't stop application: ") {
      consumers.get(generator).foreach { _.foreach { _.cancel } }
    }
    consumers += generator -> None
  }

  override def afterStart() = factory.start

  override def beforeStop() = {
    this.synchronized {
      consumers.keys.foreach { doBrokerConnectionLost(_) }
    }
    factory.stop
  }

  private def tryC[A](message: => String)(func: => A): Option[A] = {
    try {
      Some(func)
    } catch {
      case error: Exception =>
        logger.warn(message + error.getMessage, error)
        None
    }
  }
}