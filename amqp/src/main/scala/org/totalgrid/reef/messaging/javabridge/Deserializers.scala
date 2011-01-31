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
package org.totalgrid.reef.messaging.javabridge

import org.totalgrid.reef.messaging.javabridge._

import com.google.protobuf.{ ByteString, InvalidProtocolBufferException }

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

object Deserializers {

  def alarm() = new ProtoDescriptor[org.totalgrid.reef.proto.Alarms.Alarm] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Alarms.Alarm.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.Alarm.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.Alarm]
  }
  def alarmSelect() = new ProtoDescriptor[org.totalgrid.reef.proto.Alarms.AlarmSelect] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Alarms.AlarmSelect.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmSelect]
  }
  def alarmList() = new ProtoDescriptor[org.totalgrid.reef.proto.Alarms.AlarmList] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Alarms.AlarmList.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmList]
  }
  def eventConfig() = new ProtoDescriptor[org.totalgrid.reef.proto.Alarms.EventConfig] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Alarms.EventConfig.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.EventConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.EventConfig]
  }
  def heartbeatConfig() = new ProtoDescriptor[org.totalgrid.reef.proto.Application.HeartbeatConfig] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Application.HeartbeatConfig.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.HeartbeatConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.HeartbeatConfig]
  }
  def streamServicesConfig() = new ProtoDescriptor[org.totalgrid.reef.proto.Application.StreamServicesConfig] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Application.StreamServicesConfig.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.StreamServicesConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.StreamServicesConfig]
  }
  def applicationConfig() = new ProtoDescriptor[org.totalgrid.reef.proto.Application.ApplicationConfig] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Application.ApplicationConfig.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.ApplicationConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.ApplicationConfig]
  }
  def agent() = new ProtoDescriptor[org.totalgrid.reef.proto.Auth.Agent] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Auth.Agent.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Agent.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Agent]
  }
  def permission() = new ProtoDescriptor[org.totalgrid.reef.proto.Auth.Permission] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Auth.Permission.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Permission.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Permission]
  }
  def permissionSet() = new ProtoDescriptor[org.totalgrid.reef.proto.Auth.PermissionSet] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Auth.PermissionSet.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.PermissionSet.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.PermissionSet]
  }
  def authToken() = new ProtoDescriptor[org.totalgrid.reef.proto.Auth.AuthToken] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Auth.AuthToken.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.AuthToken.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.AuthToken]
  }
  def commandRequest() = new ProtoDescriptor[org.totalgrid.reef.proto.Commands.CommandRequest] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Commands.CommandRequest.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandRequest]
  }
  def commandResponse() = new ProtoDescriptor[org.totalgrid.reef.proto.Commands.CommandResponse] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Commands.CommandResponse.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandResponse.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandResponse]
  }
  def userCommandRequest() = new ProtoDescriptor[org.totalgrid.reef.proto.Commands.UserCommandRequest] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Commands.UserCommandRequest.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.UserCommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.UserCommandRequest]
  }
  def commandAccess() = new ProtoDescriptor[org.totalgrid.reef.proto.Commands.CommandAccess] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Commands.CommandAccess.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandAccess.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandAccess]
  }
  def event() = new ProtoDescriptor[org.totalgrid.reef.proto.Events.Event] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Events.Event.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Event.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Event]
  }
  def eventSelect() = new ProtoDescriptor[org.totalgrid.reef.proto.Events.EventSelect] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Events.EventSelect.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventSelect]
  }
  def eventList() = new ProtoDescriptor[org.totalgrid.reef.proto.Events.EventList] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Events.EventList.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventList]
  }
  def log() = new ProtoDescriptor[org.totalgrid.reef.proto.Events.Log] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Events.Log.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Log.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Log]
  }
  def foo() = new ProtoDescriptor[org.totalgrid.reef.proto.Example.Foo] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Example.Foo.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Example.Foo.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Example.Foo]
  }
  def ipPort() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.IpPort] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.IpPort.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.IpPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.IpPort]
  }
  def serialPort() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.SerialPort] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.SerialPort.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.SerialPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.SerialPort]
  }
  def port() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.Port] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.Port.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.Port.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.Port]
  }
  def communicationEndpointRouting() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointRouting]
  }
  def frontEndProcessor() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.FrontEndProcessor] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.FrontEndProcessor.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.FrontEndProcessor.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.FrontEndProcessor]
  }
  def endpointOwnership() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.EndpointOwnership] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.EndpointOwnership.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.EndpointOwnership.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.EndpointOwnership]
  }
  def communicationEndpointConfig() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointConfig]
  }
  def communicationEndpointConnection() = new ProtoDescriptor[org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommunicationEndpointConnection]
  }
  def measMap() = new ProtoDescriptor[org.totalgrid.reef.proto.Mapping.MeasMap] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Mapping.MeasMap.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.MeasMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.MeasMap]
  }
  def commandMap() = new ProtoDescriptor[org.totalgrid.reef.proto.Mapping.CommandMap] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Mapping.CommandMap.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.CommandMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.CommandMap]
  }
  def indexMapping() = new ProtoDescriptor[org.totalgrid.reef.proto.Mapping.IndexMapping] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Mapping.IndexMapping.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.IndexMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.IndexMapping]
  }
  def detailQual() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.DetailQual] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.DetailQual.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.DetailQual.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.DetailQual]
  }
  def quality() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.Quality] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.Quality.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Quality.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Quality]
  }
  def measurement() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.Measurement] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.Measurement.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Measurement.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Measurement]
  }
  def measurementBatch() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementBatch] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.MeasurementBatch.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementBatch.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementBatch]
  }
  def measArchiveUnit() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.MeasArchiveUnit.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchiveUnit.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit]
  }
  def measArchive() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchive] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.MeasArchive.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchive.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchive]
  }
  def measurementSnapshot() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.MeasurementSnapshot.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementSnapshot.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot]
  }
  def measurementHistory() = new ProtoDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementHistory] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Measurements.MeasurementHistory.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementHistory.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementHistory]
  }
  def relationship() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.Relationship] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.Relationship.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Relationship.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Relationship]
  }
  def entity() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.Entity] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.Entity.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Entity.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Entity]
  }
  def entityEdge() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.EntityEdge] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.EntityEdge.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.EntityEdge.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.EntityEdge]
  }
  def point() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.Point] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.Point.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Point.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Point]
  }
  def command() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.Command] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.Command.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Command.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Command]
  }
  def configFile() = new ProtoDescriptor[org.totalgrid.reef.proto.Model.ConfigFile] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Model.ConfigFile.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.ConfigFile.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.ConfigFile]
  }
  def statusSnapshot() = new ProtoDescriptor[StatusSnapshot] {
    def deserializeString(bytes: ByteString) = StatusSnapshot.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = StatusSnapshot.parseFrom(bytes)
    def getKlass = classOf[StatusSnapshot]
  }
  def measOverride() = new ProtoDescriptor[MeasOverride] {
    def deserializeString(bytes: ByteString) = MeasOverride.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = MeasOverride.parseFrom(bytes)
    def getKlass = classOf[MeasOverride]
  }
  def action() = new ProtoDescriptor[Action] {
    def deserializeString(bytes: ByteString) = Action.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = Action.parseFrom(bytes)
    def getKlass = classOf[Action]
  }
  def linearTransform() = new ProtoDescriptor[LinearTransform] {
    def deserializeString(bytes: ByteString) = LinearTransform.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = LinearTransform.parseFrom(bytes)
    def getKlass = classOf[LinearTransform]
  }
  def eventGeneration() = new ProtoDescriptor[EventGeneration] {
    def deserializeString(bytes: ByteString) = EventGeneration.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = EventGeneration.parseFrom(bytes)
    def getKlass = classOf[EventGeneration]
  }
  def trigger() = new ProtoDescriptor[Trigger] {
    def deserializeString(bytes: ByteString) = Trigger.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = Trigger.parseFrom(bytes)
    def getKlass = classOf[Trigger]
  }
  def triggerSet() = new ProtoDescriptor[TriggerSet] {
    def deserializeString(bytes: ByteString) = TriggerSet.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = TriggerSet.parseFrom(bytes)
    def getKlass = classOf[TriggerSet]
  }
  def analogLimit() = new ProtoDescriptor[AnalogLimit] {
    def deserializeString(bytes: ByteString) = AnalogLimit.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = AnalogLimit.parseFrom(bytes)
    def getKlass = classOf[AnalogLimit]
  }
  def measurementProcessingRouting() = new ProtoDescriptor[MeasurementProcessingRouting] {
    def deserializeString(bytes: ByteString) = MeasurementProcessingRouting.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = MeasurementProcessingRouting.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingRouting]
  }
  def measurementProcessingConnection() = new ProtoDescriptor[MeasurementProcessingConnection] {
    def deserializeString(bytes: ByteString) = MeasurementProcessingConnection.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = MeasurementProcessingConnection.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingConnection]
  }
  def requestHeader() = new ProtoDescriptor[org.totalgrid.reef.proto.Envelope.RequestHeader] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Envelope.RequestHeader.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.RequestHeader.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.RequestHeader]
  }
  def serviceRequest() = new ProtoDescriptor[org.totalgrid.reef.proto.Envelope.ServiceRequest] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Envelope.ServiceRequest.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceRequest]
  }
  def serviceResponse() = new ProtoDescriptor[org.totalgrid.reef.proto.Envelope.ServiceResponse] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Envelope.ServiceResponse.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceResponse.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceResponse]
  }
  def serviceNotification() = new ProtoDescriptor[org.totalgrid.reef.proto.Envelope.ServiceNotification] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Envelope.ServiceNotification.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Envelope.ServiceNotification.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Envelope.ServiceNotification]
  }
  def measSim() = new ProtoDescriptor[org.totalgrid.reef.proto.SimMapping.MeasSim] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.SimMapping.MeasSim.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.MeasSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.MeasSim]
  }
  def commandSim() = new ProtoDescriptor[org.totalgrid.reef.proto.SimMapping.CommandSim] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.SimMapping.CommandSim.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.CommandSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.CommandSim]
  }
  def simulatorMapping() = new ProtoDescriptor[org.totalgrid.reef.proto.SimMapping.SimulatorMapping] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.SimMapping.SimulatorMapping.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.SimulatorMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.SimulatorMapping]
  }
  def fieldDescr() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.FieldDescr] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.FieldDescr.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.FieldDescr.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.FieldDescr]
  }
  def field() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.Field] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.Field.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Field.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Field]
  }
  def tagControl() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.TagControl] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.TagControl.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagControl.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagControl]
  }
  def tagType() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.TagType] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.TagType.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagType.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagType]
  }
  def tag() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.Tag] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.Tag.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Tag.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Tag]
  }
  def tagQuery() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.TagQuery] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.TagQuery.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagQuery.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagQuery]
  }
  def tagList() = new ProtoDescriptor[org.totalgrid.reef.proto.Tags.TagList] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Tags.TagList.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagList]
  }
  def attribute() = new ProtoDescriptor[org.totalgrid.reef.proto.Utils.Attribute] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Utils.Attribute.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.Attribute.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.Attribute]
  }
  def attributeList() = new ProtoDescriptor[org.totalgrid.reef.proto.Utils.AttributeList] {
    def deserializeString(bytes: ByteString) = org.totalgrid.reef.proto.Utils.AttributeList.parseFrom(bytes)
    def deserializeBytes(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.AttributeList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.AttributeList]
  }
}
