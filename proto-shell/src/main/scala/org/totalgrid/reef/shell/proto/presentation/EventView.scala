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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.client.service.proto.Events.Event
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import java.text.SimpleDateFormat

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.service.proto.Alarms.{ Alarm, EventConfig }

import org.totalgrid.reef.util.Table

object EventView {

  def printEventTable(events: List[Event]) = {
    Table.printTable(header, events.map(row(_)))
  }

  def header = {
    "Id" :: "Type" :: "Alarm" :: "Sev" :: "User" :: "Entity" :: "Message" :: "Time" :: Nil
  }

  val dateFormat = new SimpleDateFormat("HH:mm:ss MM-dd-yy")
  def timeString(time: Option[Long]) = time.map { t => dateFormat.format(new java.util.Date(t)) }.getOrElse("")

  def row(e: Event) = {
    e.getId.getValue :: e.getEventType :: e.getAlarm.toString :: e.getSeverity.toString :: e.getUserId :: e.entity.name.getOrElse("") :: e.getRendered :: timeString(e.time) :: Nil
  }

  private def getValueAsString(attr: Attribute): String = {
    attr.getVtype match {
      case Attribute.Type.BOOL => attr.getValueBool.toString
      case Attribute.Type.SINT64 => attr.getValueSint64.toString
      case Attribute.Type.DOUBLE => attr.getValueDouble.toString
      case Attribute.Type.BYTES => "Data of length: " + attr.getValueBytes.size()
      case Attribute.Type.STRING => attr.getValueString.toString
    }
  }

  def printInspect(e: Event) = {

    val argumentLines: List[List[String]] = e.args.attribute.map { jargs =>
      val args = jargs.toList
      ("Arguments" :: args.size.toString :: Nil) :: args.map { optAttr =>
        val attr = optAttr.get
        attr.getName :: getValueAsString(attr) :: attr.getVtype.toString :: Nil
      }
    }.getOrElse(Nil)

    val lines: List[List[String]] =
      ("Id" :: e.getId.getValue :: Nil) ::
        ("Type" :: e.getEventType :: Nil) ::
        ("Alarm" :: e.getAlarm.toString :: Nil) ::
        ("Sev" :: e.getSeverity.toString :: Nil) ::
        ("User" :: e.getUserId :: Nil) ::
        ("Subsystem" :: e.getSubsystem :: Nil) ::
        ("Entity" :: e.entity.name.getOrElse("") :: Nil) ::
        ("Rendered" :: e.getRendered :: Nil) ::
        ("Time" :: timeString(e.time) :: Nil) ::
        ("Device Time" :: timeString(e.deviceTime) :: Nil) ::
        argumentLines

    Table.justifyColumns(lines).foreach(line => println(line.mkString(" | ")))
  }

  def printConfigTable(configs: List[EventConfig]) = {
    Table.printTable(configHeader, configs.map(configRow(_)))
  }

  def configHeader = {
    "EventType" :: "Dest" :: "Sev" :: "Audible" :: "Resources" :: "BuiltIn" :: Nil
  }

  def configRow(e: EventConfig) = {
    e.getEventType ::
      e.getDesignation.toString ::
      e.getSeverity.toString ::
      (e.getAlarmState == Alarm.State.UNACK_AUDIBLE).toString ::
      e.getResource ::
      e.getBuiltIn.toString :: Nil
  }
}