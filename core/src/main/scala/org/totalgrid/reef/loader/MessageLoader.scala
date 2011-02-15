/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.loader

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.{ Logging }
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.api.scalaclient.SyncOperations

/**
 * Load the message configuration for alarms, events, and logs.
 */

class MessageLoader(client: SyncOperations) extends Logging {

  /**
   * Load this equipment node and all children. Create edges to connect the children.
   * Return equipmentPointUnits - map of points to units
   */
  def load(model: MessageModel): Unit = {

    var messageCount = 0L

    info("Start")

    model.getMessageSet.toList.foreach(ms => {
      println("Loading messageModel: processing messageSet '" + ms.getName + "'")
      messageCount += loadMessageSet(ms, "", None, None, None)
    })

    // Could have messages that are not contained within a messageSet
    val messages = model.getMessage.toList
    messages.foreach(loadMessage(_, "", None, None, None))
    messageCount += messages.length

    println("Loading messageModel: loaded " + messageCount + " messages")
    info("Loading messageModel: loaded " + messageCount + " messages")
    info("End")
  }

  /**
   * Load this messageSet and return the number of messages loaded.
   */
  private def loadMessageSet(
    messageSet: MessageSet,
    namePrefix: String,
    severity: Option[Int],
    typ: Option[String],
    state: Option[String]): Long = {

    val name = namePrefix + messageSet.getName
    val childPrefix = name + "."
    var messageCount = 0L

    trace("load messageSet: '" + name + "'")

    val thisSeverity = getAttribute[Int](name, messageSet, _.isSetSeverity, _.getSeverity, severity, "severity")
    val thisTyp = getAttribute[String](name, messageSet, _.isSetType, _.getType, typ, "type")
    val thisState = getAttribute[String](name, messageSet, _.isSetState, _.getState, state, "state")

    val messages = messageSet.getMessage.toList
    messages.foreach(loadMessage(_, childPrefix, thisSeverity, thisTyp, thisState))
    messageCount += messages.length

    messageSet.getMessageSet.toList.foreach(ms => messageCount += loadMessageSet(ms, childPrefix, thisSeverity, thisTyp, thisState))

    messageCount
  }

  /**
   * Load this message
   */
  private def loadMessage(
    message: Message,
    namePrefix: String,
    severity: Option[Int],
    typ: Option[String],
    state: Option[String]): Unit = {

    val name = namePrefix + message.getName
    val childPrefix = name + "."
    var messageCount = 0L

    val thisSeverity = getAttributeEx[Int](name, message, _.isSetSeverity, _.getSeverity, severity, "severity")
    val thisTyp = getAttributeEx[String](name, message, _.isSetType, _.getType, typ, "type")

    if (!message.isSetValue)
      throw new Exception("message '" + name + "' is missing required message text. Ex: <message name=\"someAlarm\">Text for message goes here.</message>")
    val resourceString = message.getValue

    thisTyp match {
      case "ALARM" =>
        val thisState = getAttributeEx[String](name, message, _.isSetState, _.getState, state, "state")
        client.putOrThrow(toEventConfig(name, thisSeverity, thisTyp, thisState, resourceString))
      case _ =>
        client.putOrThrow(toEventConfig(name, thisSeverity, thisTyp, "", resourceString))
    }

  }

  /**
   * Get a message attribute or the default value if unavailable.
   */
  def getAttribute[A](
    name: String,
    messageSet: MessageSet,
    isSet: (MessageSet) => Boolean,
    get: (MessageSet) => A,
    default: Option[A],
    attributeName: String): Option[A] = {

    val value = isSet(messageSet) match {
      case true => Some(get(messageSet))
      case false => default
    }
    value
  }

  /**
   * Get an attribute on this message or the default value.
   * Throw an exception if it's not available.
   */
  def getAttributeEx[A](
    name: String,
    message: Message,
    isSet: (Message) => Boolean,
    get: (Message) => A,
    default: Option[A],
    attributeName: String): A = {

    val value: A = isSet(message) match {
      case true => get(message)
      case false =>
        default match {
          case Some(v) => v
          case _ => throw new Exception("message '" + name + "' is missing required attribute '" + attributeName + "'.")
        }
    }
    value
  }

  /**
   * Create an EventConfig proto
   */
  def toEventConfig(name: String, severity: Int, designation: String, state: String, resource: String): EventConfig = {

    val des = designation match {
      case "ALARM" => EventConfig.Designation.ALARM
      case "EVENT" => EventConfig.Designation.EVENT
      case "LOG" => EventConfig.Designation.LOG
      case d: String => throw new Exception("message '" + name + "' has an invalide type=\"" + d + "\"")
    }

    val proto = EventConfig.newBuilder
      .setEventType(name)
      .setSeverity(severity)
      .setDesignation(des)
      .setResource(resource)

    // if it's an alarm, set the alarm state
    if (des == EventConfig.Designation.ALARM) {
      // Don't allow REMOVED  or unknown states.
      val st = state match {
        case "UNACK_AUDIBLE" => Alarm.State.UNACK_AUDIBLE
        case "UNACK_SILENT" => Alarm.State.UNACK_SILENT
        case "ACKNOWLEDGED" => Alarm.State.ACKNOWLEDGED
        case s: String => throw new Exception("message '" + name + "' has an invalide state=\"" + s + "\"")
      }

      proto.setAlarmState(st)
    }

    proto.build
  }
}