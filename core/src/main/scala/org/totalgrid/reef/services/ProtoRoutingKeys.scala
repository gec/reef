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
package org.totalgrid.reef.services

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.Application._
import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.ProcessStatus._

import org.totalgrid.reef.util.Optional._

object ProtoRoutingKeys {

  def generateRoutingKey(l: List[Option[Any]]): String = {
    l.map { o =>
      // replace any dots with dashes in the names
      o.getOrElse("*").toString.replace(".", "-")
    }.mkString(".")
  }
  implicit def optionsToKey(l: List[Option[Any]]): String = generateRoutingKey(l)

  def routingKey(proto: CommunicationEndpointConfig): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: CommunicationEndpointConfig): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasName, proto.getName) :: Nil
  }

  def routingKey(proto: ApplicationConfig): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: ApplicationConfig): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasInstanceName, proto.getInstanceName) :: Nil
  }

  def routingKey(proto: Command): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: Command): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasName, proto.getName) ::
      hasGet(proto.hasEntity, proto.getEntity.getUid) :: Nil
  }

  def routingKey(proto: CommunicationEndpointConnection): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: CommunicationEndpointConnection): List[Option[Any]] = {
    hasGet(proto.hasFrontEnd, proto.getFrontEnd.hasUid, proto.getFrontEnd.getUid) ::
      hasGet(proto.hasUid, proto.getUid) :: Nil
  }

  def routingKey(proto: ConfigFile): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: ConfigFile): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasName, proto.getName) :: Nil
  }

  def routingKey(proto: EventConfig): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: EventConfig): List[Option[Any]] = {
    hasGet(proto.hasEventType, proto.getEventType) :: Nil
  }

  def routingKey(proto: FrontEndProcessor): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: FrontEndProcessor): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) :: Nil
  }

  def routingKey(proto: Point): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: Point): List[Option[Any]] = {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasName, proto.getName) ::
      hasGet(proto.hasEntity, proto.getEntity.getUid) :: Nil
  }

  def routingKey(proto: Port): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: Port): List[Option[Any]] = {
    hasGet(proto.hasName, proto.getName) ::
      hasGet(proto.hasIp, proto.getIp.getNetwork) ::
      hasGet(proto.hasSerial, proto.getSerial.getLocation) :: Nil
  }

  def routingKey(proto: MeasurementProcessingConnection): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: MeasurementProcessingConnection): List[Option[Any]] = {
    hasGet(proto.hasMeasProc, proto.getMeasProc.hasUid, proto.getMeasProc.getUid) ::
      hasGet(proto.hasUid, proto.getUid) :: Nil
  }

  def routingKey(proto: MeasOverride): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: MeasOverride): List[Option[Any]] = {
    // TODO: use the OptionalStruct implicts here instead
    hasGet(proto.hasPoint, proto.getPoint.hasLogicalNode, proto.getPoint.getLogicalNode.hasUid, proto.getPoint.getLogicalNode.getUid) ::
      hasGet(proto.hasPoint, proto.getPoint.hasName, proto.getPoint.getName) :: Nil
  }

  /*def routingKey(proto: Trigger): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: Trigger): List[Option[Any]] = {
    hasGet(proto.hasPoint, proto.getPoint.hasEntity, proto.getPoint.getEntity.hasUid, proto.getPoint.getEntity.getUid) ::
      hasGet(proto.hasPoint, proto.getPoint.hasName, proto.getPoint.getName) ::
      hasGet(proto.hasTriggerName, proto.getTriggerName) :: Nil
  }*/

  def hasCountGet[T](has: => Boolean, count: => Int, get: Int => T): Option[T] = if (has && count > 0) Some(get(0)) else None

  def routingKey(proto: EventList): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: EventList): List[Option[Any]] = {
    hasCountGet(proto.hasSelect, proto.getSelect.getEventTypeCount, proto.getSelect.getEventType) ::
      hasCountGet(proto.hasSelect, proto.getSelect.getSeverityCount, proto.getSelect.getSeverity) ::
      hasCountGet(proto.hasSelect, proto.getSelect.getSubsystemCount, proto.getSelect.getSubsystem) :: hasCountGet(proto.hasSelect, proto.getSelect.getUserIdCount, proto.getSelect.getUserId) ::
      hasCountGet(proto.hasSelect, proto.getSelect.getEntityCount, proto.getSelect.getEntity) :: Nil
  }

  def routingKey(proto: Event): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: Event): List[Option[Any]] = {
    hasGet(proto.hasEventType, proto.getEventType) ::
      hasGet(proto.hasSeverity, proto.getSeverity) ::
      hasGet(proto.hasSubsystem, proto.getSubsystem) ::
      hasGet(proto.hasUserId, proto.getUserId) ::
      hasGet(proto.hasEntity, proto.getEntity.getUid) :: Nil
  }

  def routingKey(proto: StatusSnapshot): String = routingKeyOptions(proto)
  def routingKeyOptions(proto: StatusSnapshot): List[Option[Any]] = {
    hasGet(proto.hasInstanceName, proto.getInstanceName) :: Nil
  }

}