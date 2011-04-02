package org.totalgrid.reef.proto

import org.totalgrid.reef.api.ITypeDescriptor

import org.totalgrid.reef.proto.Application._
import org.totalgrid.reef.proto.Commands._
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

  def alarm() = new ITypeDescriptor[org.totalgrid.reef.proto.Alarms.Alarm] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.Alarm): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.Alarm.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.Alarm]
  }
  def alarmSelect() = new ITypeDescriptor[org.totalgrid.reef.proto.Alarms.AlarmSelect] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.AlarmSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmSelect]
  }
  def alarmList() = new ITypeDescriptor[org.totalgrid.reef.proto.Alarms.AlarmList] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.AlarmList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.AlarmList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.AlarmList]
  }
  def eventConfig() = new ITypeDescriptor[org.totalgrid.reef.proto.Alarms.EventConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Alarms.EventConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Alarms.EventConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Alarms.EventConfig]
  }
  def heartbeatConfig() = new ITypeDescriptor[org.totalgrid.reef.proto.Application.HeartbeatConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.HeartbeatConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.HeartbeatConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.HeartbeatConfig]
  }
  def streamServicesConfig() = new ITypeDescriptor[org.totalgrid.reef.proto.Application.StreamServicesConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.StreamServicesConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.StreamServicesConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.StreamServicesConfig]
  }
  def applicationConfig() = new ITypeDescriptor[org.totalgrid.reef.proto.Application.ApplicationConfig] {
    def serialize(typ: org.totalgrid.reef.proto.Application.ApplicationConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Application.ApplicationConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Application.ApplicationConfig]
  }
  def agent() = new ITypeDescriptor[org.totalgrid.reef.proto.Auth.Agent] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.Agent): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Agent.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Agent]
  }
  def permission() = new ITypeDescriptor[org.totalgrid.reef.proto.Auth.Permission] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.Permission): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.Permission.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.Permission]
  }
  def permissionSet() = new ITypeDescriptor[org.totalgrid.reef.proto.Auth.PermissionSet] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.PermissionSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.PermissionSet.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.PermissionSet]
  }
  def authToken() = new ITypeDescriptor[org.totalgrid.reef.proto.Auth.AuthToken] {
    def serialize(typ: org.totalgrid.reef.proto.Auth.AuthToken): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Auth.AuthToken.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Auth.AuthToken]
  }
  def userCommandRequest() = new ITypeDescriptor[org.totalgrid.reef.proto.Commands.UserCommandRequest] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.UserCommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.UserCommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.UserCommandRequest]
  }
  def commandAccess() = new ITypeDescriptor[org.totalgrid.reef.proto.Commands.CommandAccess] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandAccess): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandAccess.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandAccess]
  }
  def commandRequest() = new ITypeDescriptor[org.totalgrid.reef.proto.Commands.CommandRequest] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandRequest): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandRequest.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandRequest]
  }
  def commandResponse() = new ITypeDescriptor[org.totalgrid.reef.proto.Commands.CommandResponse] {
    def serialize(typ: org.totalgrid.reef.proto.Commands.CommandResponse): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Commands.CommandResponse.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Commands.CommandResponse]
  }
  def endpointState() = new ITypeDescriptor[org.totalgrid.reef.proto.Communications.EndpointState] {
    def serialize(typ: org.totalgrid.reef.proto.Communications.EndpointState): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Communications.EndpointState.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Communications.EndpointState]
  }
  def channelState() = new ITypeDescriptor[org.totalgrid.reef.proto.Communications.ChannelState] {
    def serialize(typ: org.totalgrid.reef.proto.Communications.ChannelState): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Communications.ChannelState.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Communications.ChannelState]
  }
  def endpointStatus() = new ITypeDescriptor[org.totalgrid.reef.proto.Communications.EndpointStatus] {
    def serialize(typ: org.totalgrid.reef.proto.Communications.EndpointStatus): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Communications.EndpointStatus.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Communications.EndpointStatus]
  }
  def channelStatus() = new ITypeDescriptor[org.totalgrid.reef.proto.Communications.ChannelStatus] {
    def serialize(typ: org.totalgrid.reef.proto.Communications.ChannelStatus): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Communications.ChannelStatus.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Communications.ChannelStatus]
  }
  def event() = new ITypeDescriptor[org.totalgrid.reef.proto.Events.Event] {
    def serialize(typ: org.totalgrid.reef.proto.Events.Event): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Event.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Event]
  }
  def eventSelect() = new ITypeDescriptor[org.totalgrid.reef.proto.Events.EventSelect] {
    def serialize(typ: org.totalgrid.reef.proto.Events.EventSelect): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventSelect.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventSelect]
  }
  def eventList() = new ITypeDescriptor[org.totalgrid.reef.proto.Events.EventList] {
    def serialize(typ: org.totalgrid.reef.proto.Events.EventList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.EventList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.EventList]
  }
  def log() = new ITypeDescriptor[org.totalgrid.reef.proto.Events.Log] {
    def serialize(typ: org.totalgrid.reef.proto.Events.Log): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Events.Log.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Events.Log]
  }
  def ipPort() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.IpPort] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.IpPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.IpPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.IpPort]
  }
  def serialPort() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.SerialPort] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.SerialPort): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.SerialPort.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.SerialPort]
  }
  def commChannel() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.CommChannel] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommChannel): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommChannel.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommChannel]
  }
  def commEndpointRouting() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.CommEndpointRouting] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommEndpointRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommEndpointRouting.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommEndpointRouting]
  }
  def frontEndProcessor() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.FrontEndProcessor] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.FrontEndProcessor): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.FrontEndProcessor.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.FrontEndProcessor]
  }
  def endpointOwnership() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.EndpointOwnership] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.EndpointOwnership): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.EndpointOwnership.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.EndpointOwnership]
  }
  def commEndpointConfig() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.CommEndpointConfig] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommEndpointConfig): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommEndpointConfig.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommEndpointConfig]
  }
  def commEndpointConnection() = new ITypeDescriptor[org.totalgrid.reef.proto.FEP.CommEndpointConnection] {
    def serialize(typ: org.totalgrid.reef.proto.FEP.CommEndpointConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.FEP.CommEndpointConnection.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.FEP.CommEndpointConnection]
  }
  def measMap() = new ITypeDescriptor[org.totalgrid.reef.proto.Mapping.MeasMap] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.MeasMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.MeasMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.MeasMap]
  }
  def commandMap() = new ITypeDescriptor[org.totalgrid.reef.proto.Mapping.CommandMap] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.CommandMap): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.CommandMap.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.CommandMap]
  }
  def indexMapping() = new ITypeDescriptor[org.totalgrid.reef.proto.Mapping.IndexMapping] {
    def serialize(typ: org.totalgrid.reef.proto.Mapping.IndexMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Mapping.IndexMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Mapping.IndexMapping]
  }
  def detailQual() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.DetailQual] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.DetailQual): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.DetailQual.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.DetailQual]
  }
  def quality() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.Quality] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.Quality): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Quality.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Quality]
  }
  def measurement() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.Measurement] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.Measurement): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.Measurement.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.Measurement]
  }
  def measurementBatch() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementBatch] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementBatch): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementBatch.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementBatch]
  }
  def measArchiveUnit() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasArchiveUnit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchiveUnit.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit]
  }
  def measArchive() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasArchive] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasArchive): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasArchive.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasArchive]
  }
  def measurementSnapshot() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementSnapshot.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot]
  }
  def measurementHistory() = new ITypeDescriptor[org.totalgrid.reef.proto.Measurements.MeasurementHistory] {
    def serialize(typ: org.totalgrid.reef.proto.Measurements.MeasurementHistory): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Measurements.MeasurementHistory.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Measurements.MeasurementHistory]
  }
  def entity() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.Entity] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Entity): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Entity.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Entity]
  }
  def relationship() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.Relationship] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Relationship): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Relationship.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Relationship]
  }
  def entityEdge() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.EntityEdge] {
    def serialize(typ: org.totalgrid.reef.proto.Model.EntityEdge): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.EntityEdge.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.EntityEdge]
  }
  def entityAttributes() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.EntityAttributes] {
    def serialize(typ: org.totalgrid.reef.proto.Model.EntityAttributes): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.EntityAttributes.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.EntityAttributes]
  }
  def point() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.Point] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Point): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Point.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Point]
  }
  def command() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.Command] {
    def serialize(typ: org.totalgrid.reef.proto.Model.Command): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.Command.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.Command]
  }
  def configFile() = new ITypeDescriptor[org.totalgrid.reef.proto.Model.ConfigFile] {
    def serialize(typ: org.totalgrid.reef.proto.Model.ConfigFile): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Model.ConfigFile.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Model.ConfigFile]
  }
  def statusSnapshot() = new ITypeDescriptor[StatusSnapshot] {
    def serialize(typ: StatusSnapshot): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = StatusSnapshot.parseFrom(bytes)
    def getKlass = classOf[StatusSnapshot]
  }
  def measOverride() = new ITypeDescriptor[MeasOverride] {
    def serialize(typ: MeasOverride): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasOverride.parseFrom(bytes)
    def getKlass = classOf[MeasOverride]
  }
  def action() = new ITypeDescriptor[Action] {
    def serialize(typ: Action): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Action.parseFrom(bytes)
    def getKlass = classOf[Action]
  }
  def linearTransform() = new ITypeDescriptor[LinearTransform] {
    def serialize(typ: LinearTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = LinearTransform.parseFrom(bytes)
    def getKlass = classOf[LinearTransform]
  }
  def eventGeneration() = new ITypeDescriptor[EventGeneration] {
    def serialize(typ: EventGeneration): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = EventGeneration.parseFrom(bytes)
    def getKlass = classOf[EventGeneration]
  }
  def trigger() = new ITypeDescriptor[Trigger] {
    def serialize(typ: Trigger): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Trigger.parseFrom(bytes)
    def getKlass = classOf[Trigger]
  }
  def triggerSet() = new ITypeDescriptor[TriggerSet] {
    def serialize(typ: TriggerSet): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = TriggerSet.parseFrom(bytes)
    def getKlass = classOf[TriggerSet]
  }
  def analogLimit() = new ITypeDescriptor[AnalogLimit] {
    def serialize(typ: AnalogLimit): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = AnalogLimit.parseFrom(bytes)
    def getKlass = classOf[AnalogLimit]
  }
  def boolEnumTransform() = new ITypeDescriptor[BoolEnumTransform] {
    def serialize(typ: BoolEnumTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = BoolEnumTransform.parseFrom(bytes)
    def getKlass = classOf[BoolEnumTransform]
  }
  def intEnumTransform() = new ITypeDescriptor[IntEnumTransform] {
    def serialize(typ: IntEnumTransform): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = IntEnumTransform.parseFrom(bytes)
    def getKlass = classOf[IntEnumTransform]
  }
  def intToString() = new ITypeDescriptor[IntToString] {
    def serialize(typ: IntToString): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = IntToString.parseFrom(bytes)
    def getKlass = classOf[IntToString]
  }
  def measurementProcessingRouting() = new ITypeDescriptor[MeasurementProcessingRouting] {
    def serialize(typ: MeasurementProcessingRouting): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingRouting.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingRouting]
  }
  def measurementProcessingConnection() = new ITypeDescriptor[MeasurementProcessingConnection] {
    def serialize(typ: MeasurementProcessingConnection): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = MeasurementProcessingConnection.parseFrom(bytes)
    def getKlass = classOf[MeasurementProcessingConnection]
  }
  def measSim() = new ITypeDescriptor[org.totalgrid.reef.proto.SimMapping.MeasSim] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.MeasSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.MeasSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.MeasSim]
  }
  def commandSim() = new ITypeDescriptor[org.totalgrid.reef.proto.SimMapping.CommandSim] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.CommandSim): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.CommandSim.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.CommandSim]
  }
  def simulatorMapping() = new ITypeDescriptor[org.totalgrid.reef.proto.SimMapping.SimulatorMapping] {
    def serialize(typ: org.totalgrid.reef.proto.SimMapping.SimulatorMapping): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.SimMapping.SimulatorMapping.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.SimMapping.SimulatorMapping]
  }
  def fieldDescr() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.FieldDescr] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.FieldDescr): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.FieldDescr.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.FieldDescr]
  }
  def field() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.Field] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.Field): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Field.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Field]
  }
  def tagControl() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.TagControl] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagControl): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagControl.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagControl]
  }
  def tagType() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.TagType] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagType): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagType.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagType]
  }
  def tag() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.Tag] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.Tag): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.Tag.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.Tag]
  }
  def tagQuery() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.TagQuery] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagQuery): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagQuery.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagQuery]
  }
  def tagList() = new ITypeDescriptor[org.totalgrid.reef.proto.Tags.TagList] {
    def serialize(typ: org.totalgrid.reef.proto.Tags.TagList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Tags.TagList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Tags.TagList]
  }
  def attribute() = new ITypeDescriptor[org.totalgrid.reef.proto.Utils.Attribute] {
    def serialize(typ: org.totalgrid.reef.proto.Utils.Attribute): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.Attribute.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.Attribute]
  }
  def attributeList() = new ITypeDescriptor[org.totalgrid.reef.proto.Utils.AttributeList] {
    def serialize(typ: org.totalgrid.reef.proto.Utils.AttributeList): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = org.totalgrid.reef.proto.Utils.AttributeList.parseFrom(bytes)
    def getKlass = classOf[org.totalgrid.reef.proto.Utils.AttributeList]
  }
}
