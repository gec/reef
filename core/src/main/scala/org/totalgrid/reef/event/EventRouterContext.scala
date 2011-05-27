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
package org.totalgrid.reef.event

import scala.collection.mutable.HashMap
import org.totalgrid.reef.app.ServiceContext
import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.util.{ Localizer, Logging }

/**
 *   Convert event severity to a corresponding log level
 */
/*
object Severities {
  private val map = Array[Int]()
  map(Severity.CRITICAL.getNumber) = Level.ERROR.getNumber
  map(Severity.MAJOR.getNumber) = Level.ERROR.getNumber
  map(Severity.MINOR.getNumber) = Level.WARNING.getNumber
  map(Severity.INFORM.getNumber) = Level.INFO.getNumber

  def toLevel(severity: Severity): Level = {
    try {
      Level.valueOf(map(severity.getNumber))
    } catch {
      case ex => Level.ERROR
    }
  }
}
*/

/**
 * Receive raw events and publish them as processed events.
 *
 * For those events tagged as Logs, publish them to the raw log channel.
 *
 * @author flint
 *
 */
class EventRouterContext(publishEvent: Event => Unit,
  publishLog: Log => Unit,
  storeEvent: Event => Unit)
    extends ServiceContext[EventConfig]
    with Localizer {

  /**
   *  Map of EventType to EventConfig
   *
   *  When we're looking for an EventConfig and it's not listed,
   *  use the default severity of CRITICAL. This should not happen when the system
   *  is configured properly.
   *
   *  FUTURE: We're storing protobufs because that's what we get off the
   *  wire. A case class would be much lighter weight, but the protobufs are already
   *  created and we're not talking about a lot of objects. If the accesors are
   *  found to be slow (no evidence), then we should store case classes.
   */
  class EventConfigMap extends HashMap[String, EventConfig]() {
    override def default(key: String): EventConfig = {
      EventConfig.newBuilder
        .setEventType(key)
        .setSeverity(1)
        .setDesignation(EventConfig.Designation.ALARM)
        .build
    }
  }
  val eventConfigs = new EventConfigMap()

  this.register(() => notifyReady)

  /**
   *  The ServiceAdapter calls this after we have received all EventConfigs from
   *  the initial startup query.
   */
  def notifyReady: Unit = {
    val l = Log.newBuilder
      .setTime(0)
      .setSubsystem("System")
      .setLevel(Level.INFO)
      .setMessage("EventRouter started")
      .build
    publishLog(l)
  }

  /**
   *  Process a raw Event.
   *  If it's really an event, set the severity, store in DB, and publish it as a processed event
   *
   * @param event       Raw event proto
   */
  def process(event: Event): Unit = {
    val eventConfig = eventConfigs(event.getEventType())
    val severity = eventConfig.getSeverity()

    eventConfig.getDesignation match {
      case EventConfig.Designation.ALARM => { // TODO: this is just the event code. fix it!

        // Copy the event and set the severity
        //
        val e = event.toBuilder()
          .setSeverity(severity)
          .build

        storeEvent(e)
        publishEvent(e)
      }
      case EventConfig.Designation.EVENT => {

        // Copy the event and set the severity
        //
        val e = event.toBuilder()
          .setSeverity(severity)
          .build

        storeEvent(e)
        publishEvent(e)
      }
      case EventConfig.Designation.LOG => {
        // Localize the event, convert it to a log and publish it.
        //
        val subsystem = event.getSubsystem
        val mr = getMessageResource(subsystem, event.getEventType)

        val l = Log.newBuilder
          .setTime(event.getTime)
          .setSubsystem(subsystem)
          .setLevel(Level.EVENT)
          .build
        //TODO: .setMessage(mr(event.getArgs.getValueList().toArray))

        publishLog(l)
      }
    }
  }

  // Handle EventConfig messages via the ServiceContext
  //
  def add(ec: EventConfig) = eventConfigs += (ec.getEventType -> ec)
  def remove(ec: EventConfig) = eventConfigs -= ec.getEventType
  def modify(ec: EventConfig) = add(ec)
  def subscribed(list: List[EventConfig]) {
    eventConfigs.clear
    list.foreach { ec => add(ec) }
  }

}
