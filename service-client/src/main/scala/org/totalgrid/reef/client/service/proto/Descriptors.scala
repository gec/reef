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
package org.totalgrid.reef.client.service.proto

import org.totalgrid.reef.client.types.TypeDescriptor

import org.totalgrid.reef.client.service.proto.Application._
import org.totalgrid.reef.client.service.proto.Commands._
import org.totalgrid.reef.client.service.proto.FEP._
import org.totalgrid.reef.client.service.proto.Mapping._
import org.totalgrid.reef.client.service.proto.Measurements._
import org.totalgrid.reef.client.service.proto.ProcessStatus._
import org.totalgrid.reef.client.service.proto.Alarms._
import org.totalgrid.reef.client.service.proto.Events._
import org.totalgrid.reef.client.service.proto.Processing._
import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.Auth._

object Descriptors {

  def alarm() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Alarms.Alarm] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Alarms.Alarm): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Alarms.Alarm.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Alarms.Alarm]
    def id = "alarm"
  }
  def alarmSelect() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Alarms.AlarmSelect] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Alarms.AlarmSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Alarms.AlarmSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Alarms.AlarmSelect]
    def id = "alarm_select"
  }
  def alarmList() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Alarms.AlarmList] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Alarms.AlarmList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Alarms.AlarmList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Alarms.AlarmList]
    def id = "alarm_list"
  }
  def eventConfig() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Alarms.EventConfig] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Alarms.EventConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Alarms.EventConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig]
    def id = "event_config"
  }
  def heartbeatConfig() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Application.HeartbeatConfig] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Application.HeartbeatConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Application.HeartbeatConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Application.HeartbeatConfig]
    def id = "heartbeat_config"
  }
  def applicationConfig() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Application.ApplicationConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Application.ApplicationConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig]
    def id = "application_config"
  }
  def agent() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Auth.Agent] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Auth.Agent): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Auth.Agent.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Auth.Agent]
    def id = "agent"
  }
  def permission() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Auth.Permission] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Auth.Permission): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Auth.Permission.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Auth.Permission]
    def id = "permission"
  }
  def permissionSet() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Auth.PermissionSet] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Auth.PermissionSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Auth.PermissionSet.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Auth.PermissionSet]
    def id = "permission_set"
  }
  def authToken() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Auth.AuthToken] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Auth.AuthToken): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Auth.AuthToken.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Auth.AuthToken]
    def id = "auth_token"
  }
  def userCommandRequest() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest]
    def id = "user_command_request"
  }
  def commandLock() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Commands.CommandLock] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Commands.CommandLock): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Commands.CommandLock.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Commands.CommandLock]
    def id = "command_lock"
  }
  def commandRequest() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Commands.CommandRequest] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Commands.CommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Commands.CommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Commands.CommandRequest]
    def id = "command_request"
  }
  def event() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Events.Event] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Events.Event): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Events.Event.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Events.Event]
    def id = "event"
  }
  def eventSelect() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Events.EventSelect] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Events.EventSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Events.EventSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Events.EventSelect]
    def id = "event_select"
  }
  def eventList() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Events.EventList] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Events.EventList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Events.EventList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Events.EventList]
    def id = "event_list"
  }
  def log() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Events.Log] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Events.Log): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Events.Log.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Events.Log]
    def id = "log"
  }
  def ipPort() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.IpPort] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.IpPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.IpPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.IpPort]
    def id = "ip_port"
  }
  def serialPort() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.SerialPort] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.SerialPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.SerialPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.SerialPort]
    def id = "serial_port"
  }
  def commChannel() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.CommChannel] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.CommChannel): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.CommChannel.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.CommChannel]
    def id = "comm_channel"
  }
  def commEndpointRouting() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.CommEndpointRouting] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.CommEndpointRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.CommEndpointRouting.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.CommEndpointRouting]
    def id = "comm_endpoint_routing"
  }
  def frontEndProcessor() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor]
    def id = "front_end_processor"
  }
  def endpointOwnership() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.EndpointOwnership] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.EndpointOwnership): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.EndpointOwnership.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointOwnership]
    def id = "endpoint_ownership"
  }
  def endpoint() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.Endpoint] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.Endpoint): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.Endpoint.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint]
    def id = "endpoint"
  }
  def endpointConnection() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.FEP.EndpointConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection]
    def id = "endpoint_connection"
  }
  def measMap() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Mapping.MeasMap] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Mapping.MeasMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Mapping.MeasMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Mapping.MeasMap]
    def id = "meas_map"
  }
  def commandMap() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Mapping.CommandMap] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Mapping.CommandMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Mapping.CommandMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Mapping.CommandMap]
    def id = "command_map"
  }
  def indexMapping() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Mapping.IndexMapping] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Mapping.IndexMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Mapping.IndexMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Mapping.IndexMapping]
    def id = "index_mapping"
  }
  def detailQual() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.DetailQual] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.DetailQual): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.DetailQual.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.DetailQual]
    def id = "detail_qual"
  }
  def quality() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.Quality] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.Quality): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.Quality.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.Quality]
    def id = "quality"
  }
  def measurement() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.Measurement] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.Measurement): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.Measurement.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement]
    def id = "measurement"
  }
  def measurementBatch() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch]
    def id = "measurement_batch"
  }
  def measArchiveUnit() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.MeasArchiveUnit] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.MeasArchiveUnit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.MeasArchiveUnit.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasArchiveUnit]
    def id = "meas_archive_unit"
  }
  def measArchive() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.MeasArchive] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.MeasArchive): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.MeasArchive.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasArchive]
    def id = "meas_archive"
  }
  def measurementSnapshot() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot]
    def id = "measurement_snapshot"
  }
  def measurementHistory() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Measurements.MeasurementHistory] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Measurements.MeasurementHistory): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Measurements.MeasurementHistory.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasurementHistory]
    def id = "measurement_history"
  }
  def reefUUID() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.ReefUUID] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.ReefUUID): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.ReefUUID.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.ReefUUID]
    def id = "reef_uuid"
  }
  def reefID() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.ReefID] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.ReefID): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.ReefID.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.ReefID]
    def id = "reef_id"
  }
  def entity() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.Entity] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.Entity): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.Entity.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.Entity]
    def id = "entity"
  }
  def relationship() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.Relationship] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.Relationship): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.Relationship.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.Relationship]
    def id = "relationship"
  }
  def entityEdge() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.EntityEdge] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.EntityEdge): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.EntityEdge.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.EntityEdge]
    def id = "entity_edge"
  }
  def entityAttributes() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.EntityAttributes] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.EntityAttributes): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.EntityAttributes.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes]
    def id = "entity_attributes"
  }
  def point() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.Point] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.Point): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.Point.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.Point]
    def id = "point"
  }
  def command() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.Command] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.Command): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.Command.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.Command]
    def id = "command"
  }
  def configFile() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Model.ConfigFile] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Model.ConfigFile): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Model.ConfigFile.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile]
    def id = "config_file"
  }
  def statusSnapshot() = new TypeDescriptor[StatusSnapshot] {
    def serialize(typ: StatusSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = StatusSnapshot.parseFrom(bytes)
    def getKlass = classOf[StatusSnapshot]
    def id = "status_snapshot"
  }
  def measOverride() = new TypeDescriptor[MeasOverride] {
    def serialize(typ: MeasOverride): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasOverride.parseFrom(bytes)
    def getKlass = classOf[MeasOverride]
    def id = "meas_override"
  }
  def action() = new TypeDescriptor[Action] {
    def serialize(typ: Action): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Action.parseFrom(bytes)
    def getKlass = classOf[Action]
    def id = "action"
  }
  def linearTransform() = new TypeDescriptor[LinearTransform] {
    def serialize(typ: LinearTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = LinearTransform.parseFrom(bytes)
    def getKlass = classOf[LinearTransform]
    def id = "linear_transform"
  }
  def eventGeneration() = new TypeDescriptor[EventGeneration] {
    def serialize(typ: EventGeneration): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = EventGeneration.parseFrom(bytes)
    def getKlass = classOf[EventGeneration]
    def id = "event_generation"
  }
  def trigger() = new TypeDescriptor[Trigger] {
    def serialize(typ: Trigger): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Trigger.parseFrom(bytes)
    def getKlass = classOf[Trigger]
    def id = "trigger"
  }
  def triggerSet() = new TypeDescriptor[TriggerSet] {
    def serialize(typ: TriggerSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = TriggerSet.parseFrom(bytes)
    def getKlass = classOf[TriggerSet]
    def id = "trigger_set"
  }
  def analogLimit() = new TypeDescriptor[AnalogLimit] {
    def serialize(typ: AnalogLimit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = AnalogLimit.parseFrom(bytes)
    def getKlass = classOf[AnalogLimit]
    def id = "analog_limit"
  }
  def boolEnumTransform() = new TypeDescriptor[BoolEnumTransform] {
    def serialize(typ: BoolEnumTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = BoolEnumTransform.parseFrom(bytes)
    def getKlass = classOf[BoolEnumTransform]
    def id = "bool_enum_transform"
  }
  def intEnumTransform() = new TypeDescriptor[IntEnumTransform] {
    def serialize(typ: IntEnumTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = IntEnumTransform.parseFrom(bytes)
    def getKlass = classOf[IntEnumTransform]
    def id = "int_enum_transform"
  }
  def intToString() = new TypeDescriptor[IntToString] {
    def serialize(typ: IntToString): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = IntToString.parseFrom(bytes)
    def getKlass = classOf[IntToString]
    def id = "int_to_string"
  }
  def measurementProcessingRouting() = new TypeDescriptor[MeasurementProcessingRouting] {
    def serialize(typ: MeasurementProcessingRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingRouting.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingRouting]
    def id = "measurement_processing_routing"
  }
  def measurementProcessingConnection() = new TypeDescriptor[MeasurementProcessingConnection] {
    def serialize(typ: MeasurementProcessingConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingConnection.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingConnection]
    def id = "measurement_processing_connection"
  }
  def measSim() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.SimMapping.MeasSim] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.SimMapping.MeasSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.SimMapping.MeasSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.SimMapping.MeasSim]
    def id = "meas_sim"
  }
  def commandSim() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.SimMapping.CommandSim] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.SimMapping.CommandSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.SimMapping.CommandSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.SimMapping.CommandSim]
    def id = "command_sim"
  }
  def simulatorMapping() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.SimMapping.SimulatorMapping] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.SimMapping.SimulatorMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.SimMapping.SimulatorMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.SimMapping.SimulatorMapping]
    def id = "simulator_mapping"
  }
  def attribute() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Utils.Attribute] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Utils.Attribute): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Utils.Attribute.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Utils.Attribute]
    def id = "attribute"
  }
  def attributeList() = new TypeDescriptor[org.totalgrid.reef.client.service.proto.Utils.AttributeList] {
    def serialize(typ: org.totalgrid.reef.client.service.proto.Utils.AttributeList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.client.service.proto.Utils.AttributeList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.client.service.proto.Utils.AttributeList]
    def id = "attribute_list"
  }
}
