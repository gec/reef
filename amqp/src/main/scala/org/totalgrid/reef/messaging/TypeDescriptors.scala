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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.protoapi.TypeDescriptor

import org.totalgrid.reef.proto.Application._
import org.totalgrid.reef.proto.Commands._
import org.totalgrid.reef.proto.Envelope._
import org.totalgrid.reef.proto.Example._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Mapping._
import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.ProcessStatus._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.Auth._
import org.totalgrid.reef.proto.Tags._

object Descriptors {

  def alarm() = new TypeDescriptor[org.totalgrid.reef.proto.Alarms.Alarm] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.Alarm): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.Alarm.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.Alarm]
  }
  def alarmSelect() = new TypeDescriptor[org.totalgrid.reef.proto.Alarms.AlarmSelect] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.AlarmSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmSelect]
  }
  def alarmList() = new TypeDescriptor[org.totalgrid.reef.proto.Alarms.AlarmList] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.AlarmList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmList]
  }
  def eventConfig() = new TypeDescriptor[org.totalgrid.reef.proto.Alarms.EventConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.EventConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.EventConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.EventConfig]
  }
  def heartbeatConfig() = new TypeDescriptor[org.totalgrid.reef.proto.Application.HeartbeatConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.HeartbeatConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.HeartbeatConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.HeartbeatConfig]
  }
  def streamServicesConfig() = new TypeDescriptor[org.totalgrid.reef.proto.Application.StreamServicesConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.StreamServicesConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.StreamServicesConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.StreamServicesConfig]
  }
  def applicationConfig() = new TypeDescriptor[org.totalgrid.reef.proto.Application.ApplicationConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.ApplicationConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.ApplicationConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.ApplicationConfig]
  }
  def agent() = new TypeDescriptor[org.totalgrid.reef.proto.Auth.Agent] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.Agent): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Agent.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Agent]
  }
  def permission() = new TypeDescriptor[org.totalgrid.reef.proto.Auth.Permission] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.Permission): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Permission.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Permission]
  }
  def permissionSet() = new TypeDescriptor[org.totalgrid.reef.proto.Auth.PermissionSet] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.PermissionSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.PermissionSet.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.PermissionSet]
  }
  def authToken() = new TypeDescriptor[org.totalgrid.reef.proto.Auth.AuthToken] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.AuthToken): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.AuthToken.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.AuthToken]
  }
  def commandRequest() = new TypeDescriptor[org.totalgrid.reef.proto.Commands.CommandRequest] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandRequest]
  }
  def commandResponse() = new TypeDescriptor[org.totalgrid.reef.proto.Commands.CommandResponse] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandResponse): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandResponse.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandResponse]
  }
  def userCommandRequest() = new TypeDescriptor[org.totalgrid.reef.proto.Commands.UserCommandRequest] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.UserCommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.UserCommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.UserCommandRequest]
  }
  def commandAccess() = new TypeDescriptor[org.totalgrid.reef.proto.Commands.CommandAccess] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandAccess): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandAccess.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandAccess]
  }
  def event() = new TypeDescriptor[org.totalgrid.reef.proto.Events.Event] {
    def serialize(typ: org.totalgrid.reef.proto.Events.Event): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Event.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Event]
  }
  def eventSelect() = new TypeDescriptor[org.totalgrid.reef.proto.Events.EventSelect] {
    def serialize(typ: org.totalgrid.reef.proto.Events.EventSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventSelect]
  }
  def eventList() = new TypeDescriptor[org.totalgrid.reef.proto.Events.EventList] {
    def serialize(typ: org.totalgrid.reef.proto.Events.EventList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventList]
  }
  def log() = new TypeDescriptor[org.totalgrid.reef.proto.Events.Log] {
    def serialize(typ: org.totalgrid.reef.proto.Events.Log): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Log.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Log]
  }
  def foo() = new TypeDescriptor[org.totalgrid.reef.proto.Example.Foo] {
    def serialize(typ: org.totalgrid.reef.proto.Example.Foo): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Example.Foo.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Example.Foo]
  }
  def ipPort() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.IpPort] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.IpPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.IpPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.IpPort]
  }
  def serialPort() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.SerialPort] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.SerialPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.SerialPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.SerialPort]
  }
  def port() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.Port] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.Port): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.Port.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.Port]
  }
  def communicationEndpointRouting() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting]
  }
  def frontEndProcessor() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.FrontEndProcessor] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.FrontEndProcessor): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.FrontEndProcessor.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.FrontEndProcessor]
  }
  def endpointOwnership() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.EndpointOwnership] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.EndpointOwnership): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.EndpointOwnership.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.EndpointOwnership]
  }
  def communicationEndpointConfig() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig]
  }
  def communicationEndpointConnection() = new TypeDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection]
  }
  def measMap() = new TypeDescriptor[org.totalgrid.reef.proto.Mapping.MeasMap] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.MeasMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.MeasMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.MeasMap]
  }
  def commandMap() = new TypeDescriptor[org.totalgrid.reef.proto.Mapping.CommandMap] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.CommandMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.CommandMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.CommandMap]
  }
  def indexMapping() = new TypeDescriptor[org.totalgrid.reef.proto.Mapping.IndexMapping] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.IndexMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.IndexMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.IndexMapping]
  }
  def detailQual() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.DetailQual] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.DetailQual): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.DetailQual.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.DetailQual]
  }
  def quality() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.Quality] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.Quality): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Quality.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Quality]
  }
  def measurement() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.Measurement] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.Measurement): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Measurement.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Measurement]
  }
  def measurementBatch() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementBatch] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementBatch): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementBatch.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementBatch]
  }
  def measArchiveUnit() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasArchiveUnit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchiveUnit.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit]
  }
  def measArchive() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchive] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasArchive): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchive.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchive]
  }
  def measurementSnapshot() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementSnapshot.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot]
  }
  def measurementHistory() = new TypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementHistory] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementHistory): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementHistory.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementHistory]
  }
  def relationship() = new TypeDescriptor[org.totalgrid.reef.proto.Model.Relationship] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Relationship): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Relationship.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Relationship]
  }
  def entity() = new TypeDescriptor[org.totalgrid.reef.proto.Model.Entity] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Entity): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Entity.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Entity]
  }
  def entityEdge() = new TypeDescriptor[org.totalgrid.reef.proto.Model.EntityEdge] {
    def serialize(typ: org.totalgrid.reef.proto.Model.EntityEdge): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.EntityEdge.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.EntityEdge]
  }
  def point() = new TypeDescriptor[org.totalgrid.reef.proto.Model.Point] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Point): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Point.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Point]
  }
  def command() = new TypeDescriptor[org.totalgrid.reef.proto.Model.Command] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Command): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Command.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Command]
  }
  def configFile() = new TypeDescriptor[org.totalgrid.reef.proto.Model.ConfigFile] {
    def serialize(typ: org.totalgrid.reef.proto.Model.ConfigFile): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.ConfigFile.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.ConfigFile]
  }
  def statusSnapshot() = new TypeDescriptor[StatusSnapshot] {
    def serialize(typ: StatusSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = StatusSnapshot.parseFrom(bytes)
    def getKlass = classOf[StatusSnapshot]
  }
  def measOverride() = new TypeDescriptor[MeasOverride] {
    def serialize(typ: MeasOverride): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasOverride.parseFrom(bytes)
    def getKlass = classOf[MeasOverride]
  }
  def action() = new TypeDescriptor[Action] {
    def serialize(typ: Action): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Action.parseFrom(bytes)
    def getKlass = classOf[Action]
  }
  def linearTransform() = new TypeDescriptor[LinearTransform] {
    def serialize(typ: LinearTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = LinearTransform.parseFrom(bytes)
    def getKlass = classOf[LinearTransform]
  }
  def eventGeneration() = new TypeDescriptor[EventGeneration] {
    def serialize(typ: EventGeneration): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = EventGeneration.parseFrom(bytes)
    def getKlass = classOf[EventGeneration]
  }
  def trigger() = new TypeDescriptor[Trigger] {
    def serialize(typ: Trigger): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Trigger.parseFrom(bytes)
    def getKlass = classOf[Trigger]
  }
  def triggerSet() = new TypeDescriptor[TriggerSet] {
    def serialize(typ: TriggerSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = TriggerSet.parseFrom(bytes)
    def getKlass = classOf[TriggerSet]
  }
  def analogLimit() = new TypeDescriptor[AnalogLimit] {
    def serialize(typ: AnalogLimit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = AnalogLimit.parseFrom(bytes)
    def getKlass = classOf[AnalogLimit]
  }
  def measurementProcessingRouting() = new TypeDescriptor[MeasurementProcessingRouting] {
    def serialize(typ: MeasurementProcessingRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingRouting.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingRouting]
  }
  def measurementProcessingConnection() = new TypeDescriptor[MeasurementProcessingConnection] {
    def serialize(typ: MeasurementProcessingConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingConnection.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingConnection]
  }
  def requestHeader() = new TypeDescriptor[org.totalgrid.reef.proto.Envelope.RequestHeader] {
    def serialize(typ: org.totalgrid.reef.proto.Envelope.RequestHeader): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.RequestHeader.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.RequestHeader]
  }
  def serviceRequest() = new TypeDescriptor[org.totalgrid.reef.proto.Envelope.ServiceRequest] {
    def serialize(typ: org.totalgrid.reef.proto.Envelope.ServiceRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceRequest]
  }
  def serviceResponse() = new TypeDescriptor[org.totalgrid.reef.proto.Envelope.ServiceResponse] {
    def serialize(typ: org.totalgrid.reef.proto.Envelope.ServiceResponse): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceResponse.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceResponse]
  }
  def serviceNotification() = new TypeDescriptor[org.totalgrid.reef.proto.Envelope.ServiceNotification] {
    def serialize(typ: org.totalgrid.reef.proto.Envelope.ServiceNotification): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceNotification.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceNotification]
  }
  def measSim() = new TypeDescriptor[org.totalgrid.reef.proto.SimMapping.MeasSim] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.MeasSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.MeasSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.MeasSim]
  }
  def commandSim() = new TypeDescriptor[org.totalgrid.reef.proto.SimMapping.CommandSim] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.CommandSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.CommandSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.CommandSim]
  }
  def simulatorMapping() = new TypeDescriptor[org.totalgrid.reef.proto.SimMapping.SimulatorMapping] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.SimulatorMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.SimulatorMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.SimulatorMapping]
  }
  def fieldDescr() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.FieldDescr] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.FieldDescr): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.FieldDescr.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.FieldDescr]
  }
  def field() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.Field] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.Field): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Field.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Field]
  }
  def tagControl() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.TagControl] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagControl): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagControl.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagControl]
  }
  def tagType() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.TagType] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagType): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagType.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagType]
  }
  def tag() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.Tag] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.Tag): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Tag.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Tag]
  }
  def tagQuery() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.TagQuery] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagQuery): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagQuery.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagQuery]
  }
  def tagList() = new TypeDescriptor[org.totalgrid.reef.proto.Tags.TagList] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagList]
  }
  def attribute() = new TypeDescriptor[org.totalgrid.reef.proto.Utils.Attribute] {
    def serialize(typ: org.totalgrid.reef.proto.Utils.Attribute): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.Attribute.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.Attribute]
  }
  def attributeList() = new TypeDescriptor[org.totalgrid.reef.proto.Utils.AttributeList] {
    def serialize(typ: org.totalgrid.reef.proto.Utils.AttributeList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.AttributeList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.AttributeList]
  }
}
