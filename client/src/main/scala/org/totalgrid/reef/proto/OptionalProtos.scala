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
package org.totalgrid.reef.proto

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

import scala.collection.JavaConversions._
import org.totalgrid.reef.clientapi.sapi.types.Optional._

object OptionalProtos {

  implicit def proto2OptAlarmsAlarm(a: org.totalgrid.reef.proto.Alarms.Alarm): OptAlarmsAlarm = new OptAlarmsAlarm(Some(a))
  class OptAlarmsAlarm(real: Option[org.totalgrid.reef.proto.Alarms.Alarm]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val state = optionally(_.hasState, _.getState)
    val event = new OptEventsEvent(optionally(_.hasEvent, _.getEvent))
    val rendered = optionally(_.hasRendered, _.getRendered)
  }
  implicit def proto2OptAlarmsAlarmSelect(a: org.totalgrid.reef.proto.Alarms.AlarmSelect): OptAlarmsAlarmSelect = new OptAlarmsAlarmSelect(Some(a))
  class OptAlarmsAlarmSelect(real: Option[org.totalgrid.reef.proto.Alarms.AlarmSelect]) extends OptionalStruct(real) {
    val state = optionally(_.getStateList.toList)
    val eventSelect = new OptEventsEventSelect(optionally(_.hasEventSelect, _.getEventSelect))
  }
  implicit def proto2OptAlarmsAlarmList(a: org.totalgrid.reef.proto.Alarms.AlarmList): OptAlarmsAlarmList = new OptAlarmsAlarmList(Some(a))
  class OptAlarmsAlarmList(real: Option[org.totalgrid.reef.proto.Alarms.AlarmList]) extends OptionalStruct(real) {
    val select = new OptAlarmsAlarmSelect(optionally(_.hasSelect, _.getSelect))
    val alarms = optionally(_.getAlarmsList.toList.map { i => new OptAlarmsAlarm(Some(i)) })
  }
  implicit def proto2OptAlarmsEventConfig(a: org.totalgrid.reef.proto.Alarms.EventConfig): OptAlarmsEventConfig = new OptAlarmsEventConfig(Some(a))
  class OptAlarmsEventConfig(real: Option[org.totalgrid.reef.proto.Alarms.EventConfig]) extends OptionalStruct(real) {
    val eventType = optionally(_.hasEventType, _.getEventType)
    val severity = optionally(_.hasSeverity, _.getSeverity)
    val designation = optionally(_.hasDesignation, _.getDesignation)
    val alarmState = optionally(_.hasAlarmState, _.getAlarmState)
    val resource = optionally(_.hasResource, _.getResource)
    val builtIn = optionally(_.hasBuiltIn, _.getBuiltIn)
  }
  implicit def proto2OptApplicationHeartbeatConfig(a: org.totalgrid.reef.proto.Application.HeartbeatConfig): OptApplicationHeartbeatConfig = new OptApplicationHeartbeatConfig(Some(a))
  class OptApplicationHeartbeatConfig(real: Option[org.totalgrid.reef.proto.Application.HeartbeatConfig]) extends OptionalStruct(real) {
    val processId = optionally(_.hasProcessId, _.getProcessId)
    val periodMs = optionally(_.hasPeriodMs, _.getPeriodMs)
    val instanceName = optionally(_.hasInstanceName, _.getInstanceName)
  }
  implicit def proto2OptApplicationApplicationConfig(a: org.totalgrid.reef.proto.Application.ApplicationConfig): OptApplicationApplicationConfig = new OptApplicationApplicationConfig(Some(a))
  class OptApplicationApplicationConfig(real: Option[org.totalgrid.reef.proto.Application.ApplicationConfig]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val userName = optionally(_.hasUserName, _.getUserName)
    val instanceName = optionally(_.hasInstanceName, _.getInstanceName)
    val processId = optionally(_.hasProcessId, _.getProcessId)
    val network = optionally(_.hasNetwork, _.getNetwork)
    val location = optionally(_.hasLocation, _.getLocation)
    val capabilites = optionally(_.getCapabilitesList.toList)
    val heartbeatCfg = new OptApplicationHeartbeatConfig(optionally(_.hasHeartbeatCfg, _.getHeartbeatCfg))
    val online = optionally(_.hasOnline, _.getOnline)
    val timesOutAt = optionally(_.hasTimesOutAt, _.getTimesOutAt)
  }
  implicit def proto2OptAuthAgent(a: org.totalgrid.reef.proto.Auth.Agent): OptAuthAgent = new OptAuthAgent(Some(a))
  class OptAuthAgent(real: Option[org.totalgrid.reef.proto.Auth.Agent]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val password = optionally(_.hasPassword, _.getPassword)
    val permissionSets = optionally(_.getPermissionSetsList.toList.map { i => new OptAuthPermissionSet(Some(i)) })
  }
  implicit def proto2OptAuthPermission(a: org.totalgrid.reef.proto.Auth.Permission): OptAuthPermission = new OptAuthPermission(Some(a))
  class OptAuthPermission(real: Option[org.totalgrid.reef.proto.Auth.Permission]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val allow = optionally(_.hasAllow, _.getAllow)
    val resource = optionally(_.hasResource, _.getResource)
    val verb = optionally(_.hasVerb, _.getVerb)
  }
  implicit def proto2OptAuthPermissionSet(a: org.totalgrid.reef.proto.Auth.PermissionSet): OptAuthPermissionSet = new OptAuthPermissionSet(Some(a))
  class OptAuthPermissionSet(real: Option[org.totalgrid.reef.proto.Auth.PermissionSet]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val defaultExpirationTime = optionally(_.hasDefaultExpirationTime, _.getDefaultExpirationTime)
    val permissions = optionally(_.getPermissionsList.toList.map { i => new OptAuthPermission(Some(i)) })
  }
  implicit def proto2OptAuthAuthToken(a: org.totalgrid.reef.proto.Auth.AuthToken): OptAuthAuthToken = new OptAuthAuthToken(Some(a))
  class OptAuthAuthToken(real: Option[org.totalgrid.reef.proto.Auth.AuthToken]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val agent = new OptAuthAgent(optionally(_.hasAgent, _.getAgent))
    val loginLocation = optionally(_.hasLoginLocation, _.getLoginLocation)
    val permissionSets = optionally(_.getPermissionSetsList.toList.map { i => new OptAuthPermissionSet(Some(i)) })
    val token = optionally(_.hasToken, _.getToken)
    val expirationTime = optionally(_.hasExpirationTime, _.getExpirationTime)
  }
  implicit def proto2OptCommandsUserCommandRequest(a: org.totalgrid.reef.proto.Commands.UserCommandRequest): OptCommandsUserCommandRequest = new OptCommandsUserCommandRequest(Some(a))
  class OptCommandsUserCommandRequest(real: Option[org.totalgrid.reef.proto.Commands.UserCommandRequest]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val commandRequest = new OptCommandsCommandRequest(optionally(_.hasCommandRequest, _.getCommandRequest))
    val status = optionally(_.hasStatus, _.getStatus)
    val user = optionally(_.hasUser, _.getUser)
    val timeoutMs = optionally(_.hasTimeoutMs, _.getTimeoutMs)
  }
  implicit def proto2OptCommandsCommandAccess(a: org.totalgrid.reef.proto.Commands.CommandAccess): OptCommandsCommandAccess = new OptCommandsCommandAccess(Some(a))
  class OptCommandsCommandAccess(real: Option[org.totalgrid.reef.proto.Commands.CommandAccess]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val commands = optionally(_.getCommandsList.toList)
    val access = optionally(_.hasAccess, _.getAccess)
    val expireTime = optionally(_.hasExpireTime, _.getExpireTime)
    val user = optionally(_.hasUser, _.getUser)
  }
  implicit def proto2OptCommandsCommandRequest(a: org.totalgrid.reef.proto.Commands.CommandRequest): OptCommandsCommandRequest = new OptCommandsCommandRequest(Some(a))
  class OptCommandsCommandRequest(real: Option[org.totalgrid.reef.proto.Commands.CommandRequest]) extends OptionalStruct(real) {
    val name = optionally(_.hasName, _.getName)
    val correlationId = optionally(_.hasCorrelationId, _.getCorrelationId)
    val _type = optionally(_.hasType, _.getType)
    val intVal = optionally(_.hasIntVal, _.getIntVal)
    val doubleVal = optionally(_.hasDoubleVal, _.getDoubleVal)
  }
  implicit def proto2OptCommandsCommandResponse(a: org.totalgrid.reef.proto.Commands.CommandResponse): OptCommandsCommandResponse = new OptCommandsCommandResponse(Some(a))
  class OptCommandsCommandResponse(real: Option[org.totalgrid.reef.proto.Commands.CommandResponse]) extends OptionalStruct(real) {
    val correlationId = optionally(_.getCorrelationId)
    val status = optionally(_.getStatus)
  }
  implicit def proto2OptEventsEvent(a: org.totalgrid.reef.proto.Events.Event): OptEventsEvent = new OptEventsEvent(Some(a))
  class OptEventsEvent(real: Option[org.totalgrid.reef.proto.Events.Event]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val eventType = optionally(_.hasEventType, _.getEventType)
    val alarm = optionally(_.hasAlarm, _.getAlarm)
    val time = optionally(_.hasTime, _.getTime)
    val deviceTime = optionally(_.hasDeviceTime, _.getDeviceTime)
    val severity = optionally(_.hasSeverity, _.getSeverity)
    val subsystem = optionally(_.hasSubsystem, _.getSubsystem)
    val userId = optionally(_.hasUserId, _.getUserId)
    val entity = new OptModelEntity(optionally(_.hasEntity, _.getEntity))
    val args = new OptUtilsAttributeList(optionally(_.hasArgs, _.getArgs))
    val rendered = optionally(_.hasRendered, _.getRendered)
  }
  implicit def proto2OptEventsEventSelect(a: org.totalgrid.reef.proto.Events.EventSelect): OptEventsEventSelect = new OptEventsEventSelect(Some(a))
  class OptEventsEventSelect(real: Option[org.totalgrid.reef.proto.Events.EventSelect]) extends OptionalStruct(real) {
    val eventType = optionally(_.getEventTypeList.toList)
    val timeFrom = optionally(_.hasTimeFrom, _.getTimeFrom)
    val timeTo = optionally(_.hasTimeTo, _.getTimeTo)
    val severity = optionally(_.getSeverityList.toList)
    val severityOrHigher = optionally(_.hasSeverityOrHigher, _.getSeverityOrHigher)
    val subsystem = optionally(_.getSubsystemList.toList)
    val userId = optionally(_.getUserIdList.toList)
    val entity = optionally(_.getEntityList.toList.map { i => new OptModelEntity(Some(i)) })
    val limit = optionally(_.hasLimit, _.getLimit)
    val ascending = optionally(_.hasAscending, _.getAscending)
  }
  implicit def proto2OptEventsEventList(a: org.totalgrid.reef.proto.Events.EventList): OptEventsEventList = new OptEventsEventList(Some(a))
  class OptEventsEventList(real: Option[org.totalgrid.reef.proto.Events.EventList]) extends OptionalStruct(real) {
    val select = new OptEventsEventSelect(optionally(_.hasSelect, _.getSelect))
    val events = optionally(_.getEventsList.toList.map { i => new OptEventsEvent(Some(i)) })
  }
  implicit def proto2OptEventsLog(a: org.totalgrid.reef.proto.Events.Log): OptEventsLog = new OptEventsLog(Some(a))
  class OptEventsLog(real: Option[org.totalgrid.reef.proto.Events.Log]) extends OptionalStruct(real) {
    val time = optionally(_.getTime)
    val level = optionally(_.getLevel)
    val subsystem = optionally(_.getSubsystem)
    val fileName = optionally(_.hasFileName, _.getFileName)
    val lineNumber = optionally(_.hasLineNumber, _.getLineNumber)
    val message = optionally(_.getMessage)
  }
  implicit def proto2OptFEPIpPort(a: org.totalgrid.reef.proto.FEP.IpPort): OptFEPIpPort = new OptFEPIpPort(Some(a))
  class OptFEPIpPort(real: Option[org.totalgrid.reef.proto.FEP.IpPort]) extends OptionalStruct(real) {
    val address = optionally(_.getAddress)
    val port = optionally(_.getPort)
    val mode = optionally(_.hasMode, _.getMode)
    val network = optionally(_.hasNetwork, _.getNetwork)
  }
  implicit def proto2OptFEPSerialPort(a: org.totalgrid.reef.proto.FEP.SerialPort): OptFEPSerialPort = new OptFEPSerialPort(Some(a))
  class OptFEPSerialPort(real: Option[org.totalgrid.reef.proto.FEP.SerialPort]) extends OptionalStruct(real) {
    val location = optionally(_.getLocation)
    val portName = optionally(_.getPortName)
    val baudRate = optionally(_.hasBaudRate, _.getBaudRate)
    val stopBits = optionally(_.hasStopBits, _.getStopBits)
    val dataBits = optionally(_.hasDataBits, _.getDataBits)
    val parity = optionally(_.hasParity, _.getParity)
    val flow = optionally(_.hasFlow, _.getFlow)
  }
  implicit def proto2OptFEPCommChannel(a: org.totalgrid.reef.proto.FEP.CommChannel): OptFEPCommChannel = new OptFEPCommChannel(Some(a))
  class OptFEPCommChannel(real: Option[org.totalgrid.reef.proto.FEP.CommChannel]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val ip = new OptFEPIpPort(optionally(_.hasIp, _.getIp))
    val serial = new OptFEPSerialPort(optionally(_.hasSerial, _.getSerial))
    val state = optionally(_.hasState, _.getState)
  }
  implicit def proto2OptFEPCommEndpointRouting(a: org.totalgrid.reef.proto.FEP.CommEndpointRouting): OptFEPCommEndpointRouting = new OptFEPCommEndpointRouting(Some(a))
  class OptFEPCommEndpointRouting(real: Option[org.totalgrid.reef.proto.FEP.CommEndpointRouting]) extends OptionalStruct(real) {
    val serviceRoutingKey = optionally(_.hasServiceRoutingKey, _.getServiceRoutingKey)
  }
  implicit def proto2OptFEPFrontEndProcessor(a: org.totalgrid.reef.proto.FEP.FrontEndProcessor): OptFEPFrontEndProcessor = new OptFEPFrontEndProcessor(Some(a))
  class OptFEPFrontEndProcessor(real: Option[org.totalgrid.reef.proto.FEP.FrontEndProcessor]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val protocols = optionally(_.getProtocolsList.toList)
    val appConfig = new OptApplicationApplicationConfig(optionally(_.hasAppConfig, _.getAppConfig))
  }
  implicit def proto2OptFEPEndpointOwnership(a: org.totalgrid.reef.proto.FEP.EndpointOwnership): OptFEPEndpointOwnership = new OptFEPEndpointOwnership(Some(a))
  class OptFEPEndpointOwnership(real: Option[org.totalgrid.reef.proto.FEP.EndpointOwnership]) extends OptionalStruct(real) {
    val points = optionally(_.getPointsList.toList)
    val commands = optionally(_.getCommandsList.toList)
  }
  implicit def proto2OptFEPCommEndpointConfig(a: org.totalgrid.reef.proto.FEP.CommEndpointConfig): OptFEPCommEndpointConfig = new OptFEPCommEndpointConfig(Some(a))
  class OptFEPCommEndpointConfig(real: Option[org.totalgrid.reef.proto.FEP.CommEndpointConfig]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val entity = new OptModelEntity(optionally(_.hasEntity, _.getEntity))
    val protocol = optionally(_.hasProtocol, _.getProtocol)
    val channel = new OptFEPCommChannel(optionally(_.hasChannel, _.getChannel))
    val ownerships = new OptFEPEndpointOwnership(optionally(_.hasOwnerships, _.getOwnerships))
    val configFiles = optionally(_.getConfigFilesList.toList.map { i => new OptModelConfigFile(Some(i)) })
    val dataSource = optionally(_.hasDataSource, _.getDataSource)
  }
  implicit def proto2OptFEPCommEndpointConnection(a: org.totalgrid.reef.proto.FEP.CommEndpointConnection): OptFEPCommEndpointConnection = new OptFEPCommEndpointConnection(Some(a))
  class OptFEPCommEndpointConnection(real: Option[org.totalgrid.reef.proto.FEP.CommEndpointConnection]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val frontEnd = new OptFEPFrontEndProcessor(optionally(_.hasFrontEnd, _.getFrontEnd))
    val endpoint = new OptFEPCommEndpointConfig(optionally(_.hasEndpoint, _.getEndpoint))
    val state = optionally(_.hasState, _.getState)
    val routing = new OptFEPCommEndpointRouting(optionally(_.hasRouting, _.getRouting))
    val lastUpdate = optionally(_.hasLastUpdate, _.getLastUpdate)
    val enabled = optionally(_.hasEnabled, _.getEnabled)
  }
  implicit def proto2OptMappingMeasMap(a: org.totalgrid.reef.proto.Mapping.MeasMap): OptMappingMeasMap = new OptMappingMeasMap(Some(a))
  class OptMappingMeasMap(real: Option[org.totalgrid.reef.proto.Mapping.MeasMap]) extends OptionalStruct(real) {
    val _type = optionally(_.getType)
    val index = optionally(_.getIndex)
    val pointName = optionally(_.getPointName)
    val unit = optionally(_.hasUnit, _.getUnit)
  }
  implicit def proto2OptMappingCommandMap(a: org.totalgrid.reef.proto.Mapping.CommandMap): OptMappingCommandMap = new OptMappingCommandMap(Some(a))
  class OptMappingCommandMap(real: Option[org.totalgrid.reef.proto.Mapping.CommandMap]) extends OptionalStruct(real) {
    val _type = optionally(_.getType)
    val index = optionally(_.getIndex)
    val commandName = optionally(_.getCommandName)
    val onTime = optionally(_.hasOnTime, _.getOnTime)
    val offTime = optionally(_.hasOffTime, _.getOffTime)
    val count = optionally(_.hasCount, _.getCount)
  }
  implicit def proto2OptMappingIndexMapping(a: org.totalgrid.reef.proto.Mapping.IndexMapping): OptMappingIndexMapping = new OptMappingIndexMapping(Some(a))
  class OptMappingIndexMapping(real: Option[org.totalgrid.reef.proto.Mapping.IndexMapping]) extends OptionalStruct(real) {
    val deviceUid = optionally(_.hasDeviceUid, _.getDeviceUid)
    val measmap = optionally(_.getMeasmapList.toList.map { i => new OptMappingMeasMap(Some(i)) })
    val commandmap = optionally(_.getCommandmapList.toList.map { i => new OptMappingCommandMap(Some(i)) })
  }
  implicit def proto2OptMeasurementsDetailQual(a: org.totalgrid.reef.proto.Measurements.DetailQual): OptMeasurementsDetailQual = new OptMeasurementsDetailQual(Some(a))
  class OptMeasurementsDetailQual(real: Option[org.totalgrid.reef.proto.Measurements.DetailQual]) extends OptionalStruct(real) {
    val overflow = optionally(_.hasOverflow, _.getOverflow)
    val outOfRange = optionally(_.hasOutOfRange, _.getOutOfRange)
    val badReference = optionally(_.hasBadReference, _.getBadReference)
    val oscillatory = optionally(_.hasOscillatory, _.getOscillatory)
    val failure = optionally(_.hasFailure, _.getFailure)
    val oldData = optionally(_.hasOldData, _.getOldData)
    val inconsistent = optionally(_.hasInconsistent, _.getInconsistent)
    val inaccurate = optionally(_.hasInaccurate, _.getInaccurate)
  }
  implicit def proto2OptMeasurementsQuality(a: org.totalgrid.reef.proto.Measurements.Quality): OptMeasurementsQuality = new OptMeasurementsQuality(Some(a))
  class OptMeasurementsQuality(real: Option[org.totalgrid.reef.proto.Measurements.Quality]) extends OptionalStruct(real) {
    val validity = optionally(_.hasValidity, _.getValidity)
    val source = optionally(_.hasSource, _.getSource)
    val detailQual = new OptMeasurementsDetailQual(optionally(_.hasDetailQual, _.getDetailQual))
    val test = optionally(_.hasTest, _.getTest)
    val operatorBlocked = optionally(_.hasOperatorBlocked, _.getOperatorBlocked)
  }
  implicit def proto2OptMeasurementsMeasurement(a: org.totalgrid.reef.proto.Measurements.Measurement): OptMeasurementsMeasurement = new OptMeasurementsMeasurement(Some(a))
  class OptMeasurementsMeasurement(real: Option[org.totalgrid.reef.proto.Measurements.Measurement]) extends OptionalStruct(real) {
    val name = optionally(_.hasName, _.getName)
    val _type = optionally(_.getType)
    val intVal = optionally(_.hasIntVal, _.getIntVal)
    val doubleVal = optionally(_.hasDoubleVal, _.getDoubleVal)
    val boolVal = optionally(_.hasBoolVal, _.getBoolVal)
    val stringVal = optionally(_.hasStringVal, _.getStringVal)
    val quality = new OptMeasurementsQuality(optionally(_.getQuality))
    val unit = optionally(_.hasUnit, _.getUnit)
    val time = optionally(_.hasTime, _.getTime)
    val isDeviceTime = optionally(_.hasIsDeviceTime, _.getIsDeviceTime)
  }
  implicit def proto2OptMeasurementsMeasurementBatch(a: org.totalgrid.reef.proto.Measurements.MeasurementBatch): OptMeasurementsMeasurementBatch = new OptMeasurementsMeasurementBatch(Some(a))
  class OptMeasurementsMeasurementBatch(real: Option[org.totalgrid.reef.proto.Measurements.MeasurementBatch]) extends OptionalStruct(real) {
    val wallTime = optionally(_.getWallTime)
    val meas = optionally(_.getMeasList.toList.map { i => new OptMeasurementsMeasurement(Some(i)) })
  }
  implicit def proto2OptMeasurementsMeasArchiveUnit(a: org.totalgrid.reef.proto.Measurements.MeasArchiveUnit): OptMeasurementsMeasArchiveUnit = new OptMeasurementsMeasArchiveUnit(Some(a))
  class OptMeasurementsMeasArchiveUnit(real: Option[org.totalgrid.reef.proto.Measurements.MeasArchiveUnit]) extends OptionalStruct(real) {
    val intVal = optionally(_.hasIntVal, _.getIntVal)
    val doubleVal = optionally(_.hasDoubleVal, _.getDoubleVal)
    val boolVal = optionally(_.hasBoolVal, _.getBoolVal)
    val stringVal = optionally(_.hasStringVal, _.getStringVal)
    val quality = new OptMeasurementsQuality(optionally(_.hasQuality, _.getQuality))
    val time = optionally(_.getTime)
  }
  implicit def proto2OptMeasurementsMeasArchive(a: org.totalgrid.reef.proto.Measurements.MeasArchive): OptMeasurementsMeasArchive = new OptMeasurementsMeasArchive(Some(a))
  class OptMeasurementsMeasArchive(real: Option[org.totalgrid.reef.proto.Measurements.MeasArchive]) extends OptionalStruct(real) {
    val meas = optionally(_.getMeasList.toList.map { i => new OptMeasurementsMeasArchiveUnit(Some(i)) })
  }
  implicit def proto2OptMeasurementsMeasurementSnapshot(a: org.totalgrid.reef.proto.Measurements.MeasurementSnapshot): OptMeasurementsMeasurementSnapshot = new OptMeasurementsMeasurementSnapshot(Some(a))
  class OptMeasurementsMeasurementSnapshot(real: Option[org.totalgrid.reef.proto.Measurements.MeasurementSnapshot]) extends OptionalStruct(real) {
    val pointNames = optionally(_.getPointNamesList.toList)
    val measurements = optionally(_.getMeasurementsList.toList.map { i => new OptMeasurementsMeasurement(Some(i)) })
  }
  implicit def proto2OptMeasurementsMeasurementHistory(a: org.totalgrid.reef.proto.Measurements.MeasurementHistory): OptMeasurementsMeasurementHistory = new OptMeasurementsMeasurementHistory(Some(a))
  class OptMeasurementsMeasurementHistory(real: Option[org.totalgrid.reef.proto.Measurements.MeasurementHistory]) extends OptionalStruct(real) {
    val pointName = optionally(_.getPointName)
    val startTime = optionally(_.hasStartTime, _.getStartTime)
    val endTime = optionally(_.hasEndTime, _.getEndTime)
    val limit = optionally(_.hasLimit, _.getLimit)
    val keepNewest = optionally(_.hasKeepNewest, _.getKeepNewest)
    val sampling = optionally(_.hasSampling, _.getSampling)
    val measurements = optionally(_.getMeasurementsList.toList.map { i => new OptMeasurementsMeasurement(Some(i)) })
  }
  implicit def proto2OptModelReefUUID(a: org.totalgrid.reef.proto.Model.ReefUUID): OptModelReefUUID = new OptModelReefUUID(Some(a))
  class OptModelReefUUID(real: Option[org.totalgrid.reef.proto.Model.ReefUUID]) extends OptionalStruct(real) {
    val value = optionally(_.getValue)
  }
  implicit def proto2OptModelReefID(a: org.totalgrid.reef.proto.Model.ReefID): OptModelReefID = new OptModelReefID(Some(a))
  class OptModelReefID(real: Option[org.totalgrid.reef.proto.Model.ReefID]) extends OptionalStruct(real) {
    val value = optionally(_.getValue)
  }
  implicit def proto2OptModelEntity(a: org.totalgrid.reef.proto.Model.Entity): OptModelEntity = new OptModelEntity(Some(a))
  class OptModelEntity(real: Option[org.totalgrid.reef.proto.Model.Entity]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val types = optionally(_.getTypesList.toList)
    val name = optionally(_.hasName, _.getName)
    val relations = optionally(_.getRelationsList.toList.map { i => new OptModelRelationship(Some(i)) })
  }
  implicit def proto2OptModelRelationship(a: org.totalgrid.reef.proto.Model.Relationship): OptModelRelationship = new OptModelRelationship(Some(a))
  class OptModelRelationship(real: Option[org.totalgrid.reef.proto.Model.Relationship]) extends OptionalStruct(real) {
    val relationship = optionally(_.hasRelationship, _.getRelationship)
    val descendantOf = optionally(_.hasDescendantOf, _.getDescendantOf)
    val entities = optionally(_.getEntitiesList.toList.map { i => new OptModelEntity(Some(i)) })
    val distance = optionally(_.hasDistance, _.getDistance)
  }
  implicit def proto2OptModelEntityEdge(a: org.totalgrid.reef.proto.Model.EntityEdge): OptModelEntityEdge = new OptModelEntityEdge(Some(a))
  class OptModelEntityEdge(real: Option[org.totalgrid.reef.proto.Model.EntityEdge]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val parent = new OptModelEntity(optionally(_.hasParent, _.getParent))
    val child = new OptModelEntity(optionally(_.hasChild, _.getChild))
    val relationship = optionally(_.hasRelationship, _.getRelationship)
  }
  implicit def proto2OptModelEntityAttributes(a: org.totalgrid.reef.proto.Model.EntityAttributes): OptModelEntityAttributes = new OptModelEntityAttributes(Some(a))
  class OptModelEntityAttributes(real: Option[org.totalgrid.reef.proto.Model.EntityAttributes]) extends OptionalStruct(real) {
    val entity = new OptModelEntity(optionally(_.hasEntity, _.getEntity))
    val attributes = optionally(_.getAttributesList.toList.map { i => new OptUtilsAttribute(Some(i)) })
  }
  implicit def proto2OptModelPoint(a: org.totalgrid.reef.proto.Model.Point): OptModelPoint = new OptModelPoint(Some(a))
  class OptModelPoint(real: Option[org.totalgrid.reef.proto.Model.Point]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val endpoint = new OptModelEntity(optionally(_.hasEndpoint, _.getEndpoint))
    val entity = new OptModelEntity(optionally(_.hasEntity, _.getEntity))
    val abnormal = optionally(_.hasAbnormal, _.getAbnormal)
    val _type = optionally(_.hasType, _.getType)
    val unit = optionally(_.hasUnit, _.getUnit)
  }
  implicit def proto2OptModelCommand(a: org.totalgrid.reef.proto.Model.Command): OptModelCommand = new OptModelCommand(Some(a))
  class OptModelCommand(real: Option[org.totalgrid.reef.proto.Model.Command]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val displayName = optionally(_.hasDisplayName, _.getDisplayName)
    val endpoint = new OptModelEntity(optionally(_.hasEndpoint, _.getEndpoint))
    val entity = new OptModelEntity(optionally(_.hasEntity, _.getEntity))
    val _type = optionally(_.hasType, _.getType)
  }
  implicit def proto2OptModelConfigFile(a: org.totalgrid.reef.proto.Model.ConfigFile): OptModelConfigFile = new OptModelConfigFile(Some(a))
  class OptModelConfigFile(real: Option[org.totalgrid.reef.proto.Model.ConfigFile]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val name = optionally(_.hasName, _.getName)
    val mimeType = optionally(_.hasMimeType, _.getMimeType)
    val file = optionally(_.hasFile, _.getFile)
    val entities = optionally(_.getEntitiesList.toList.map { i => new OptModelEntity(Some(i)) })
  }
  implicit def proto2OptStatusSnapshot(a: StatusSnapshot): OptStatusSnapshot = new OptStatusSnapshot(Some(a))
  class OptStatusSnapshot(real: Option[StatusSnapshot]) extends OptionalStruct(real) {
    val processId = optionally(_.hasProcessId, _.getProcessId)
    val instanceName = optionally(_.hasInstanceName, _.getInstanceName)
    val online = optionally(_.hasOnline, _.getOnline)
    val time = optionally(_.hasTime, _.getTime)
  }
  implicit def proto2OptMeasOverride(a: MeasOverride): OptMeasOverride = new OptMeasOverride(Some(a))
  class OptMeasOverride(real: Option[MeasOverride]) extends OptionalStruct(real) {
    val point = new OptModelPoint(optionally(_.getPoint))
    val meas = new OptMeasurementsMeasurement(optionally(_.hasMeas, _.getMeas))
  }
  implicit def proto2OptAction(a: Action): OptAction = new OptAction(Some(a))
  class OptAction(real: Option[Action]) extends OptionalStruct(real) {
    val actionName = optionally(_.getActionName)
    val _type = optionally(_.hasType, _.getType)
    val disabled = optionally(_.hasDisabled, _.getDisabled)
    val linearTransform = new OptLinearTransform(optionally(_.hasLinearTransform, _.getLinearTransform))
    val qualityAnnotation = new OptMeasurementsQuality(optionally(_.hasQualityAnnotation, _.getQualityAnnotation))
    val stripValue = optionally(_.hasStripValue, _.getStripValue)
    val setBool = optionally(_.hasSetBool, _.getSetBool)
    val setUnit = optionally(_.hasSetUnit, _.getSetUnit)
    val event = new OptEventGeneration(optionally(_.hasEvent, _.getEvent))
    val boolTransform = new OptBoolEnumTransform(optionally(_.hasBoolTransform, _.getBoolTransform))
    val intTransform = new OptIntEnumTransform(optionally(_.hasIntTransform, _.getIntTransform))
  }
  implicit def proto2OptLinearTransform(a: LinearTransform): OptLinearTransform = new OptLinearTransform(Some(a))
  class OptLinearTransform(real: Option[LinearTransform]) extends OptionalStruct(real) {
    val scale = optionally(_.hasScale, _.getScale)
    val offset = optionally(_.hasOffset, _.getOffset)
  }
  implicit def proto2OptEventGeneration(a: EventGeneration): OptEventGeneration = new OptEventGeneration(Some(a))
  class OptEventGeneration(real: Option[EventGeneration]) extends OptionalStruct(real) {
    val eventType = optionally(_.hasEventType, _.getEventType)
    val severity = optionally(_.hasSeverity, _.getSeverity)
  }
  implicit def proto2OptTrigger(a: Trigger): OptTrigger = new OptTrigger(Some(a))
  class OptTrigger(real: Option[Trigger]) extends OptionalStruct(real) {
    val triggerName = optionally(_.hasTriggerName, _.getTriggerName)
    val stopProcessingWhen = optionally(_.hasStopProcessingWhen, _.getStopProcessingWhen)
    val priority = optionally(_.hasPriority, _.getPriority)
    val actions = optionally(_.getActionsList.toList.map { i => new OptAction(Some(i)) })
    val analogLimit = new OptAnalogLimit(optionally(_.hasAnalogLimit, _.getAnalogLimit))
    val quality = new OptMeasurementsQuality(optionally(_.hasQuality, _.getQuality))
    val unit = optionally(_.hasUnit, _.getUnit)
    val valueType = optionally(_.hasValueType, _.getValueType)
    val boolValue = optionally(_.hasBoolValue, _.getBoolValue)
    val stringValue = optionally(_.hasStringValue, _.getStringValue)
    val intValue = optionally(_.hasIntValue, _.getIntValue)
  }
  implicit def proto2OptTriggerSet(a: TriggerSet): OptTriggerSet = new OptTriggerSet(Some(a))
  class OptTriggerSet(real: Option[TriggerSet]) extends OptionalStruct(real) {
    val uuid = new OptModelReefUUID(optionally(_.hasUuid, _.getUuid))
    val point = new OptModelPoint(optionally(_.hasPoint, _.getPoint))
    val triggers = optionally(_.getTriggersList.toList.map { i => new OptTrigger(Some(i)) })
  }
  implicit def proto2OptAnalogLimit(a: AnalogLimit): OptAnalogLimit = new OptAnalogLimit(Some(a))
  class OptAnalogLimit(real: Option[AnalogLimit]) extends OptionalStruct(real) {
    val upperLimit = optionally(_.hasUpperLimit, _.getUpperLimit)
    val lowerLimit = optionally(_.hasLowerLimit, _.getLowerLimit)
    val deadband = optionally(_.hasDeadband, _.getDeadband)
  }
  implicit def proto2OptBoolEnumTransform(a: BoolEnumTransform): OptBoolEnumTransform = new OptBoolEnumTransform(Some(a))
  class OptBoolEnumTransform(real: Option[BoolEnumTransform]) extends OptionalStruct(real) {
    val trueString = optionally(_.getTrueString)
    val falseString = optionally(_.getFalseString)
  }
  implicit def proto2OptIntEnumTransform(a: IntEnumTransform): OptIntEnumTransform = new OptIntEnumTransform(Some(a))
  class OptIntEnumTransform(real: Option[IntEnumTransform]) extends OptionalStruct(real) {
    val mappings = optionally(_.getMappingsList.toList.map { i => new OptIntToString(Some(i)) })
  }
  implicit def proto2OptIntToString(a: IntToString): OptIntToString = new OptIntToString(Some(a))
  class OptIntToString(real: Option[IntToString]) extends OptionalStruct(real) {
    val value = optionally(_.getValue)
    val string = optionally(_.getString)
  }
  implicit def proto2OptMeasurementProcessingRouting(a: MeasurementProcessingRouting): OptMeasurementProcessingRouting = new OptMeasurementProcessingRouting(Some(a))
  class OptMeasurementProcessingRouting(real: Option[MeasurementProcessingRouting]) extends OptionalStruct(real) {
    val serviceRoutingKey = optionally(_.hasServiceRoutingKey, _.getServiceRoutingKey)
    val processedMeasDest = optionally(_.hasProcessedMeasDest, _.getProcessedMeasDest)
    val rawEventDest = optionally(_.hasRawEventDest, _.getRawEventDest)
  }
  implicit def proto2OptMeasurementProcessingConnection(a: MeasurementProcessingConnection): OptMeasurementProcessingConnection = new OptMeasurementProcessingConnection(Some(a))
  class OptMeasurementProcessingConnection(real: Option[MeasurementProcessingConnection]) extends OptionalStruct(real) {
    val uid = new OptModelReefID(optionally(_.hasUid, _.getUid))
    val measProc = new OptApplicationApplicationConfig(optionally(_.hasMeasProc, _.getMeasProc))
    val logicalNode = new OptModelEntity(optionally(_.hasLogicalNode, _.getLogicalNode))
    val routing = new OptMeasurementProcessingRouting(optionally(_.hasRouting, _.getRouting))
    val assignedTime = optionally(_.hasAssignedTime, _.getAssignedTime)
    val readyTime = optionally(_.hasReadyTime, _.getReadyTime)
  }
  implicit def proto2OptSimMappingMeasSim(a: org.totalgrid.reef.proto.SimMapping.MeasSim): OptSimMappingMeasSim = new OptSimMappingMeasSim(Some(a))
  class OptSimMappingMeasSim(real: Option[org.totalgrid.reef.proto.SimMapping.MeasSim]) extends OptionalStruct(real) {
    val name = optionally(_.getName)
    val unit = optionally(_.getUnit)
    val _type = optionally(_.getType)
    val initial = optionally(_.hasInitial, _.getInitial)
    val min = optionally(_.hasMin, _.getMin)
    val max = optionally(_.hasMax, _.getMax)
    val maxDelta = optionally(_.hasMaxDelta, _.getMaxDelta)
    val changeChance = optionally(_.hasChangeChance, _.getChangeChance)
  }
  implicit def proto2OptSimMappingCommandSim(a: org.totalgrid.reef.proto.SimMapping.CommandSim): OptSimMappingCommandSim = new OptSimMappingCommandSim(Some(a))
  class OptSimMappingCommandSim(real: Option[org.totalgrid.reef.proto.SimMapping.CommandSim]) extends OptionalStruct(real) {
    val name = optionally(_.getName)
    val responseStatus = optionally(_.getResponseStatus)
  }
  implicit def proto2OptSimMappingSimulatorMapping(a: org.totalgrid.reef.proto.SimMapping.SimulatorMapping): OptSimMappingSimulatorMapping = new OptSimMappingSimulatorMapping(Some(a))
  class OptSimMappingSimulatorMapping(real: Option[org.totalgrid.reef.proto.SimMapping.SimulatorMapping]) extends OptionalStruct(real) {
    val delay = optionally(_.getDelay)
    val measurements = optionally(_.getMeasurementsList.toList.map { i => new OptSimMappingMeasSim(Some(i)) })
    val commands = optionally(_.getCommandsList.toList.map { i => new OptSimMappingCommandSim(Some(i)) })
  }
  implicit def proto2OptUtilsAttribute(a: org.totalgrid.reef.proto.Utils.Attribute): OptUtilsAttribute = new OptUtilsAttribute(Some(a))
  class OptUtilsAttribute(real: Option[org.totalgrid.reef.proto.Utils.Attribute]) extends OptionalStruct(real) {
    val name = optionally(_.getName)
    val vtype = optionally(_.getVtype)
    val vdescriptor = optionally(_.hasVdescriptor, _.getVdescriptor)
    val valueString = optionally(_.hasValueString, _.getValueString)
    val valueSint64 = optionally(_.hasValueSint64, _.getValueSint64)
    val valueDouble = optionally(_.hasValueDouble, _.getValueDouble)
    val valueBool = optionally(_.hasValueBool, _.getValueBool)
    val valueBytes = optionally(_.hasValueBytes, _.getValueBytes)
  }
  implicit def proto2OptUtilsAttributeList(a: org.totalgrid.reef.proto.Utils.AttributeList): OptUtilsAttributeList = new OptUtilsAttributeList(Some(a))
  class OptUtilsAttributeList(real: Option[org.totalgrid.reef.proto.Utils.AttributeList]) extends OptionalStruct(real) {
    val attribute = optionally(_.getAttributeList.toList.map { i => new OptUtilsAttribute(Some(i)) })
  }
}
