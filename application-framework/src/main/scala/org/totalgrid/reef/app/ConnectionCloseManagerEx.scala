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
import org.totalgrid.reef.broker.{ BrokerConnectionListener, BrokerConnection }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.api.japi.settings.AmqpSettings
import org.totalgrid.reef.util.{ Lifecycle, Cancelable => UCancelable }

/**
 * handles the connection to an amqp broker, passing the valid and created Connection to the
 * applicationCreator function
 */
class ConnectionCloseManagerEx(amqpSettings: AmqpSettings)
    extends BrokerConnectionListener
    with Lifecycle
    with Logging {

  private val remoteFactory = new QpidBrokerConnectionFactory(amqpSettings)

  private val exe = Executors.newScheduledThreadPool()

  private var connection = Option.empty[BrokerConnection]
  private var consumers = Map.empty[ConnectionConsumer, Option[UCancelable]]
  private var delays = Map.empty[ConnectionConsumer, Cancelable]

  def addConsumer(generator: ConnectionConsumer) = this.synchronized {
    if (consumers.get(generator).isDefined) throw new IllegalArgumentException("Consumer already added")
    val cancelable = connection.map { generator.newConnection(_, exe) }

    consumers += generator -> cancelable
  }
  def removeConsumer(generator: ConnectionConsumer) = this.synchronized {
    if (consumers.get(generator).isEmpty) throw new IllegalArgumentException("Unknown consumer")
    doBrokerConnectionLost(generator)
    consumers -= generator
  }

  def onConnectionOpened(conn: BrokerConnection) = this.synchronized {
    connection = Some(conn)
    conn.addListener(this)
    cancelDelays()
    consumers.keys.foreach { doBrokerConnectionStarted(_, conn) }
  }

  override def onDisconnect(expected: Boolean) = this.synchronized {
    connection.foreach(_.removeListener(this))
    connection = None
    cancelDelays()
    if (!expected) {
      logger.warn("Connection to broker " + amqpSettings + " lost.")
      consumers.keys.foreach { doBrokerConnectionLost(_) }
      // try reconnecting
      afterStart()
    }
  }

  private def cancelDelays() {
    delays.values.foreach { _.cancel }
    delays = Map.empty[ConnectionConsumer, Cancelable]
  }

  private def doBrokerConnectionStarted(generator: ConnectionConsumer, conn: BrokerConnection): Unit = this.synchronized {
    consumers.foreach {
      case (generator, cancelable) =>
        if (cancelable.isDefined) {
          logger.error("Still have an active application instance onConnectionOpened!")
          doBrokerConnectionLost(generator)
        }

        val application = tryC("Couldn't start consumer successfully: ") {
          generator.newConnection(conn, exe)
        }
        consumers += generator -> application

        if (application.isEmpty) {
          delays += generator -> exe.delay(5000.milliseconds) { connection.foreach(doBrokerConnectionStarted(generator, _)) }
        }
    }
  }

  private def doBrokerConnectionLost(generator: ConnectionConsumer) = this.synchronized {
    tryC("Couldn't stop application: ") {
      consumers.get(generator).foreach { _.foreach { _.cancel } }
    }
    consumers += generator -> None
  }

  override def afterStart() = this.synchronized {
    tryConnection(5000)
  }

  override def beforeStop() = this.synchronized {
    consumers.keys.foreach { doBrokerConnectionLost(_) }
    connection.foreach(_.disconnect)
    exe.terminate()
  }

  private def tryConnection(timeout: Long) {
    tryC("Couldn't connect to " + amqpSettings + ": ") {
      onConnectionOpened(remoteFactory.connect)
    }
    if (connection.isEmpty) exe.delay(timeout.milliseconds) { tryConnection(timeout * 2) }
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