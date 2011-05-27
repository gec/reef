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

import EventType.eventTypeToString
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.services.core.util.AttributeList
import org.totalgrid.reef.proto.{ Descriptors, RoutingKeys, Events }

/**
 * thick interface for the creation and publishing of logs and events.
 *
 * <h3>Localization Resource Bundle Files</h3>
 * EventType's are in org.psi.event._ in the utils directory. They reference
 * localization resources in resource files. Resource files are at
 * core/resources/\<subsystem\>\<locale\>.properties
 * For example, the FEP module's resource file is at
 * core/resources/FEP_en_US.properties
 *
 * We will have to provide more capabilities in the future to
 * search for resource files in other locations as we have more modules.
 *
 */
trait EventLogPublisher {
  /// The subsystem corresponds to the resource file.
  /// Example: FEP_en_US.properties
  val subsystem: String
  /// the sink for events
  val publishEvent: Events.Event => Unit
  /// the sink for log messages
  val publishLog: Events.Log => Unit

  /**
   * Post an event with a specified resource to the bus.
   *
   * @param EventType     Event IDs are in the util module, org.psi.event
   * @param resourceId  Associated utility resource in string form. The display
   *                    should be able to use this as a URI to display the
   *                    resource.
   *                    Ex: southeast.raleigh.station1.bay2.breaker512
   * @param args        0 or more string arguments that will be plugged into
   *                    the localization resource string. If the resource
   *                    string is "{1} happened {2}" and the args are "This",
   *                    "here", the final event message will be
   *                    "This happened here".
   */
  def event(eventType: EventType, resourceId: String, args: AttributeList): Unit = {

    // TODO: Get the current user
    val userId = "system"

    val e = Events.Event.newBuilder
      .setTime(System.currentTimeMillis) // is this GMT? (new Date).getTime is.
      .setDeviceTime(0) // TODO: the frontend needs to set this for devices/protocols tha support it.
      .setEventType(eventType)
      .setSubsystem(subsystem)
      .setUserId(userId)
    //.setEntity( ...)

    if (!args.isEmpty)
      e.setArgs(args.toProto)

    publishEvent(e.build)
  }

  /**
   * Post an event to the bus.
   *
   * @param eventType     Event IDs are in the util module, org.psi.event
   */
  def event(eventType: EventType): Unit = event(eventType, "", new AttributeList)

  /**
   * Post a prebuilt Events.Event protobuf to the bus.
   *
   * @param event       The Events protobuf that was previously built.
   */
  def event(_event: Events.Event): Unit = publishEvent(_event)

  /**
   * Post a log to the bus.
   */
  def log(level: Events.Level, message: String): Unit = {

    // TODO: Get the current user
    val userId = "system"

    val l = Events.Log.newBuilder
      .setTime(System.currentTimeMillis)
      .setSubsystem(subsystem)
      .setLevel(level)
      .setMessage(message)

    publishLog(l.build)
  }

  def event(message: String) = log(Events.Level.EVENT, message)
  def error(message: String) = log(Events.Level.ERROR, message)
  def warning(message: String) = log(Events.Level.WARNING, message)
  def info(message: String) = log(Events.Level.INFO, message)
  def interpret(message: String) = log(Events.Level.INTERPRET, message)
  def com(message: String) = log(Events.Level.COM, message)
  def debug(message: String) = log(Events.Level.DEBUG, message)
  def trace(message: String) = log(Events.Level.TRACE, message)

  /**
   * Post a prebuilt Events.Log protobuf to the bus.
   */
  def log(_log: Events.Log): Unit = publishLog(_log)
}

/**
 * Publish events and logs to the bus. Events are localized on the
 * receiver side when they are written to the log file. Logs are not
 * localized.
 *
 * <h3>Use Case</h3><pre>
 * val eventLog = new EventLogPublisher(amqp, "FEP", "raw_events", "raw_logs")     // publishers for events and logs
 *
 * ...
 * eventLog.event( EventType.System.SubsystemStarted)
 * </pre>
 */
class BusTiedEventLogPublisher(amqp: AMQPProtoFactory,
  subSystem: String,
  eventExchange: String,
  logExchange: String)
    extends EventLogPublisher {
  val subsystem = subSystem
  val publishEvent = amqp.publish(eventExchange, RoutingKeys.event, Descriptors.event.serialize)
  val publishLog = amqp.publish(logExchange, RoutingKeys.log, Descriptors.log.serialize)
}

/**
 * Blackhole event/log handler for testing and mocking purposes
 */
object SilentEventLogPublisher extends EventLogPublisher {
  val subsystem = ""
  val publishEvent = (e: Events.Event) => {}
  val publishLog = (l: Events.Log) => {}
}
