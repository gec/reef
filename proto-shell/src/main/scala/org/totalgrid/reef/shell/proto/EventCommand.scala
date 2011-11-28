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
package org.totalgrid.reef.shell.proto

import org.totalgrid.reef.shell.proto.presentation.{ EventView }

import scala.collection.JavaConversions._
import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }
import org.totalgrid.reef.proto.Utils.Attribute
import org.totalgrid.reef.proto.Alarms.{ EventConfig, Alarm }
import org.totalgrid.reef.proto.Model.ReefID

@Command(scope = "event", name = "list", description = "Prints all recent events.")
class EventListCommand extends ReefCommandSupport {

  @GogoOption(name = "-t", description = "Show only events of type, can be repeated.", required = false, multiValued = true)
  var types: java.util.List[String] = null

  @GogoOption(name = "-l", description = "Limit number of displayed events", required = false, multiValued = false)
  var limit: Int = 10

  def doCommand() = {
    val typList = Option(types).map(_.toList) getOrElse List("*")

    EventView.printEventTable(services.getRecentEvents(typList, limit).toList)
  }
}

@Command(scope = "event", name = "view", description = "Prints details for a specific event.")
class EventViewCommand extends ReefCommandSupport {

  @Argument(name = "eventId", description = "Event Id", required = true, multiValued = false)
  var eventId: String = null

  def doCommand() = {
    EventView.printInspect(services.getEvent(ReefID.newBuilder.setValue(eventId).build))
  }
}

@Command(scope = "event", name = "publish", description = "Manually create an event")
class EventPublishCommand extends ReefCommandSupport {

  @Argument(name = "eventType", description = "Event Type", required = true, multiValued = false)
  var eventType: String = null

  @GogoOption(name = "-s", description = "Subsystem", required = false, multiValued = false)
  var subsystem: String = "proto-shell"

  @GogoOption(name = "-a", description = "Arguments, need to be of the form \"name:value\"", required = false, multiValued = true)
  var arguments: java.util.List[String] = null

  @GogoOption(name = "-e", description = "Entity Name", required = false, multiValued = false)
  var entityName: String = null

  def doCommand() = {
    val entityUuid = Option(entityName).map { services.getEntityByName(_).getUuid }

    var attributes = List.empty[Attribute]
    if (arguments != null) {
      arguments.toList.foreach { arg =>
        val split = arg.split(":")
        val name = split.head
        val value = split.tail.mkString("")
        if (value.length == 0) throw new Exception("Badly formed argument: " + arg)
        val b = Attribute.newBuilder.setName(name)
        import org.totalgrid.reef.util.Conversion.convertStringToType
        convertStringToType(value) match {
          case x: Int => b.setValueSint64(x).setVtype(Attribute.Type.SINT64)
          case x: Long => b.setValueSint64(x).setVtype(Attribute.Type.SINT64)
          case x: Double => b.setValueDouble(x).setVtype(Attribute.Type.DOUBLE)
          case x: Boolean => b.setValueBool(x).setVtype(Attribute.Type.BOOL)
          case x: String => b.setValueString(x).setVtype(Attribute.Type.STRING)
          case x: Any => throw new Exception("Couldn't convert " + x + " into long, boolean, double or string: " + x.asInstanceOf[AnyRef].getClass)
        }
        attributes ::= b.build
      }
    }

    val event = entityUuid match {
      case Some(uuid) => services.publishEvent(eventType, subsystem, uuid, attributes)
      case None => services.publishEvent(eventType, subsystem, attributes)
    }

    EventView.printInspect(event)
  }
}

@Command(scope = "event-config", name = "list", description = "Prints all event configurations.")
class EventConfigListCommand extends ReefCommandSupport {

  def doCommand() = {
    EventView.printConfigTable(services.getAllEventConfigurations.toList)
  }
}

@Command(scope = "event-config", name = "view", description = "Prints a single event configuration.")
class EventConfigViewCommand extends ReefCommandSupport {

  @Argument(name = "eventType", description = "Event Type", required = true, multiValued = false)
  var eventType: String = null

  def doCommand() = {
    EventView.printConfigTable(services.getEventConfiguration(eventType) :: Nil)
  }
}

@Command(scope = "event-config", name = "delete", description = "Delete a single event configuration.")
class EventConfigDeleteCommand extends ReefCommandSupport {

  @Argument(name = "eventType", description = "Event Type", required = true, multiValued = false)
  var eventType: String = null

  def doCommand() = {
    EventView.printConfigTable(services.deleteEventConfig(services.getEventConfiguration(eventType)) :: Nil)
  }
}

@Command(scope = "event-config", name = "create", description = "Create or update a single event configuration.")
class EventConfigCreateCommand extends ReefCommandSupport {

  @Argument(name = "eventType", description = "Event Type", required = true, multiValued = false, index = 0)
  var eventType: String = null

  @Argument(name = "resourceString", description = "Resource string we use to render event messsage", required = true, multiValued = false, index = 1)
  var resourceString: String = null

  @GogoOption(name = "-s", description = "Severity (lower is more severe)", required = false, multiValued = false)
  var severity: Int = 4

  @GogoOption(name = "--alarm", description = "Treat the event as an alarm.", required = false, multiValued = false)
  var alarm: Boolean = false

  @GogoOption(name = "--log", description = "Treat the event as an log.", required = false, multiValued = false)
  var _log: Boolean = false

  @GogoOption(name = "--silent", description = "If its an alarm make it silent", required = false, multiValued = false)
  var silentAlarm: Boolean = false

  def doCommand() = {
    val designation = (alarm, _log) match {
      case (true, true) => throw new Exception("Cannot set both --alarm and --log flags.")
      case (true, false) => EventConfig.Designation.ALARM
      case (false, true) => EventConfig.Designation.LOG
      case (false, false) => EventConfig.Designation.EVENT
    }
    val config = services.setEventConfig(eventType, severity, designation, !silentAlarm, resourceString)
    EventView.printConfigTable(config :: Nil)
  }
}