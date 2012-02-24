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
package org.totalgrid.reef.httpbridge.servlets.apiproviders

import org.totalgrid.reef.httpbridge.servlets.helpers.ApiCallLibrary
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

/**
 * Auto Generated, do not alter!
 * 113 of 189 calls ported
 */
class AllScadaServiceApiCallLibrary extends ApiCallLibrary[AllScadaService] {
  override val serviceClass = classOf[AllScadaService]

  ////////////////////
  // AgentService
  ////////////////////
  single("getAgentByName", classOf[org.totalgrid.reef.client.service.proto.Auth.Agent], args => {
    val a0 = args.getString("name")
    (c) => c.getAgentByName(a0)
  })
  multi("getAgents", classOf[org.totalgrid.reef.client.service.proto.Auth.Agent], args => { (c) =>
    c.getAgents()
  })
  single("createNewAgent", classOf[org.totalgrid.reef.client.service.proto.Auth.Agent], args => {
    val a0 = args.getString("name")
    val a1 = args.getString("password")
    val a2 = args.getStrings("permissionSetNames")
    (c) => c.createNewAgent(a0, a1, a2)
  })
  // Can't encode deleteAgent : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Agent
  // Can't encode setAgentPassword : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Agent
  multi("getPermissionSets", classOf[org.totalgrid.reef.client.service.proto.Auth.PermissionSet], args => { (c) =>
    c.getPermissionSets()
  })
  single("getPermissionSet", classOf[org.totalgrid.reef.client.service.proto.Auth.PermissionSet], args => {
    val a0 = args.getString("name")
    (c) => c.getPermissionSet(a0)
  })
  // Can't encode createPermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Permission
  // Can't encode deletePermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.PermissionSet
  ////////////////////
  // AlarmService
  ////////////////////
  single("getAlarmById", classOf[org.totalgrid.reef.client.service.proto.Alarms.Alarm], args => {
    val a0 = args.getString("id")
    (c) => c.getAlarmById(a0)
  })
  multi("getActiveAlarms", classOf[org.totalgrid.reef.client.service.proto.Alarms.Alarm], args => {
    val a0 = args.getInt("limit")
    (c) => c.getActiveAlarms(a0)
  })
  subscription("subscribeToActiveAlarms", classOf[org.totalgrid.reef.client.service.proto.Alarms.Alarm], args => {
    val a0 = args.getInt("recentAlarmLimit")
    (c) => c.subscribeToActiveAlarms(a0)
  })
  multi("getActiveAlarms", classOf[org.totalgrid.reef.client.service.proto.Alarms.Alarm], args => {
    val a0 = args.getStrings("types")
    val a1 = args.getInt("recentAlarmLimit")
    (c) => c.getActiveAlarms(a0, a1)
  })
  // Can't encode getActiveAlarmsByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
  // Can't encode silenceAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
  // Can't encode acknowledgeAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
  // Can't encode removeAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
  ////////////////////
  // ApplicationService
  ////////////////////
  // Can't encode registerApplication : Can't encode type: org.totalgrid.reef.client.settings.NodeSettings
  // Can't encode unregisterApplication : Can't encode type: org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
  // Can't encode sendHeartbeat : Can't encode type: org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot
  // Can't encode sendHeartbeat : Can't encode type: org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
  // Can't encode sendApplicationOffline : Can't encode type: org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
  multi("getApplications", classOf[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig], args => { (c) =>
    c.getApplications()
  })
  optional("findApplicationByName", classOf[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig], args => {
    val a0 = args.getString("name")
    (c) => c.findApplicationByName(a0)
  })
  single("getApplicationByName", classOf[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig], args => {
    val a0 = args.getString("name")
    (c) => c.getApplicationByName(a0)
  })
  single("getApplicationByUuid", classOf[org.totalgrid.reef.client.service.proto.Application.ApplicationConfig], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getApplicationByUuid(a0)
  })
  ////////////////////
  // CalculationService
  ////////////////////
  multi("getCalculations", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => { (c) =>
    c.getCalculations()
  })
  single("getCalculationByUuid", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getCalculationByUuid(a0)
  })
  single("getCalculationForPointByName", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getString("pointName")
    (c) => c.getCalculationForPointByName(a0)
  })
  single("getCalculationForPointByUuid", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getCalculationForPointByUuid(a0)
  })
  multi("getCalculationsSourcedByEndpointByName", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getString("endpointName")
    (c) => c.getCalculationsSourcedByEndpointByName(a0)
  })
  multi("getCalculationsSourcedByEndpointByUuid", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getCalculationsSourcedByEndpointByUuid(a0)
  })
  subscription("subscribeToCalculationsSourcedByEndpointByUuid", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.subscribeToCalculationsSourcedByEndpointByUuid(a0)
  })
  // Can't encode addCalculation : Can't encode type: org.totalgrid.reef.client.service.proto.Calculations.Calculation
  single("deleteCalculation", classOf[org.totalgrid.reef.client.service.proto.Calculations.Calculation], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.deleteCalculation(a0)
  })
  // Can't encode deleteCalculation : Can't encode type: org.totalgrid.reef.client.service.proto.Calculations.Calculation
  ////////////////////
  // ClientOperations
  ////////////////////
  // Can't encode getOne : Can't serialize non-protobuf response: T
  // Can't encode findOne : Can't serialize non-protobuf response: T
  // Can't encode getMany : Can't serialize non-protobuf response: T
  // Can't encode subscribeMany : Can't serialize non-protobuf response: T
  // Can't encode deleteOne : Can't serialize non-protobuf response: T
  // Can't encode deleteMany : Can't serialize non-protobuf response: T
  // Can't encode putOne : Can't serialize non-protobuf response: T
  // Can't encode putMany : Can't serialize non-protobuf response: T
  // Can't encode postOne : Can't serialize non-protobuf response: T
  // Can't encode postMany : Can't serialize non-protobuf response: T
  ////////////////////
  // CommandService
  ////////////////////
  // Can't encode createCommandExecutionLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode createCommandExecutionLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode createCommandExecutionLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode createCommandExecutionLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode deleteCommandLock : Can't encode type: org.totalgrid.reef.client.service.proto.Commands.CommandLock
  single("deleteCommandLock", classOf[org.totalgrid.reef.client.service.proto.Commands.CommandLock], args => {
    val a0 = args.getId("commandId")
    (c) => c.deleteCommandLock(a0)
  })
  multi("clearCommandLocks", classOf[org.totalgrid.reef.client.service.proto.Commands.CommandLock], args => { (c) =>
    c.clearCommandLocks()
  })
  // Can't encode executeCommandAsControl : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode createCommandDenialLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  multi("getCommandLocks", classOf[org.totalgrid.reef.client.service.proto.Commands.CommandLock], args => { (c) =>
    c.getCommandLocks()
  })
  single("getCommandLockById", classOf[org.totalgrid.reef.client.service.proto.Commands.CommandLock], args => {
    val a0 = args.getId("id")
    (c) => c.getCommandLockById(a0)
  })
  // Can't encode findCommandLockOnCommand : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  // Can't encode getCommandLocksOnCommands : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  multi("getCommandHistory", classOf[org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest], args => { (c) =>
    c.getCommandHistory()
  })
  // Can't encode getCommandHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
  multi("getCommands", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => { (c) =>
    c.getCommands()
  })
  single("getCommandByName", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getString("name")
    (c) => c.getCommandByName(a0)
  })
  single("getCommandByUuid", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getCommandByUuid(a0)
  })
  multi("getCommandsByNames", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getStrings("names")
    (c) => c.getCommandsByNames(a0)
  })
  multi("getCommandsByUuids", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getUuids("uuids")
    (c) => c.getCommandsByUuids(a0)
  })
  multi("getCommandsOwnedByEntity", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getUuid("parentUUID")
    (c) => c.getCommandsOwnedByEntity(a0)
  })
  multi("getCommandsBelongingToEndpoint", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.getCommandsBelongingToEndpoint(a0)
  })
  multi("getCommandsThatFeedbackToPoint", classOf[org.totalgrid.reef.client.service.proto.Model.Command], args => {
    val a0 = args.getUuid("pointUuid")
    (c) => c.getCommandsThatFeedbackToPoint(a0)
  })
  // Can't encode bindCommandHandler : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionBinding
  ////////////////////
  // CommunicationChannelService
  ////////////////////
  multi("getCommunicationChannels", classOf[org.totalgrid.reef.client.service.proto.FEP.CommChannel], args => { (c) =>
    c.getCommunicationChannels()
  })
  single("getCommunicationChannelByUuid", classOf[org.totalgrid.reef.client.service.proto.FEP.CommChannel], args => {
    val a0 = args.getUuid("channelUuid")
    (c) => c.getCommunicationChannelByUuid(a0)
  })
  single("getCommunicationChannelByName", classOf[org.totalgrid.reef.client.service.proto.FEP.CommChannel], args => {
    val a0 = args.getString("channelName")
    (c) => c.getCommunicationChannelByName(a0)
  })
  // Can't encode alterCommunicationChannelState : Can't encode type: org.totalgrid.reef.client.service.proto.FEP.CommChannel.State
  multi("getEndpointsUsingChannel", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => {
    val a0 = args.getUuid("channelUuid")
    (c) => c.getEndpointsUsingChannel(a0)
  })
  ////////////////////
  // ConfigFileService
  ////////////////////
  multi("getConfigFiles", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => { (c) =>
    c.getConfigFiles()
  })
  single("getConfigFileByUuid", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getConfigFileByUuid(a0)
  })
  single("getConfigFileByName", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => {
    val a0 = args.getString("name")
    (c) => c.getConfigFileByName(a0)
  })
  optional("findConfigFileByName", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => {
    val a0 = args.getString("name")
    (c) => c.findConfigFileByName(a0)
  })
  multi("getConfigFilesUsedByEntity", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => {
    val a0 = args.getUuid("entityUuid")
    (c) => c.getConfigFilesUsedByEntity(a0)
  })
  multi("getConfigFilesUsedByEntity", classOf[org.totalgrid.reef.client.service.proto.Model.ConfigFile], args => {
    val a0 = args.getUuid("entityUuid")
    val a1 = args.getString("mimeType")
    (c) => c.getConfigFilesUsedByEntity(a0, a1)
  })
  // Can't encode createConfigFile : Can't encode type: byte[]
  // Can't encode createConfigFile : Can't encode type: byte[]
  // Can't encode createConfigFile : Can't encode type: byte[]
  // Can't encode updateConfigFile : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
  // Can't encode addConfigFileUsedByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
  // Can't encode deleteConfigFile : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
  ////////////////////
  // EndpointService
  ////////////////////
  multi("getEndpoints", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => { (c) =>
    c.getEndpoints()
  })
  single("getEndpointByName", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => {
    val a0 = args.getString("name")
    (c) => c.getEndpointByName(a0)
  })
  single("getEndpointByUuid", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.getEndpointByUuid(a0)
  })
  multi("getEndpointsByNames", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => {
    val a0 = args.getStrings("names")
    (c) => c.getEndpointsByNames(a0)
  })
  multi("getEndpointsByUuids", classOf[org.totalgrid.reef.client.service.proto.FEP.Endpoint], args => {
    val a0 = args.getUuids("endpointUuids")
    (c) => c.getEndpointsByUuids(a0)
  })
  single("disableEndpointConnection", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.disableEndpointConnection(a0)
  })
  single("enableEndpointConnection", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.enableEndpointConnection(a0)
  })
  multi("getEndpointConnections", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => { (c) =>
    c.getEndpointConnections()
  })
  subscription("subscribeToEndpointConnections", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => { (c) =>
    c.subscribeToEndpointConnections()
  })
  single("getEndpointConnectionByUuid", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.getEndpointConnectionByUuid(a0)
  })
  single("getEndpointConnectionByEndpointName", classOf[org.totalgrid.reef.client.service.proto.FEP.EndpointConnection], args => {
    val a0 = args.getString("endpointName")
    (c) => c.getEndpointConnectionByEndpointName(a0)
  })
  // Can't encode alterEndpointConnectionState : Can't encode type: org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State
  ////////////////////
  // EntityService
  ////////////////////
  multi("getEntities", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => { (c) =>
    c.getEntities()
  })
  single("getEntityByUuid", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getEntityByUuid(a0)
  })
  single("getEntityByName", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getString("name")
    (c) => c.getEntityByName(a0)
  })
  multi("getEntitiesByUuids", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuids("uuids")
    (c) => c.getEntitiesByUuids(a0)
  })
  multi("getEntitiesByNames", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getStrings("names")
    (c) => c.getEntitiesByNames(a0)
  })
  optional("findEntityByName", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getString("name")
    (c) => c.findEntityByName(a0)
  })
  multi("getEntitiesWithType", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getString("typeName")
    (c) => c.getEntitiesWithType(a0)
  })
  multi("getEntitiesWithTypes", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getStrings("types")
    (c) => c.getEntitiesWithTypes(a0)
  })
  multi("getEntityRelatedChildrenOfType", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getString("relationship")
    val a2 = args.getString("typeName")
    (c) => c.getEntityRelatedChildrenOfType(a0, a1, a2)
  })
  multi("getEntityImmediateChildren", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getString("relationship")
    (c) => c.getEntityImmediateChildren(a0, a1)
  })
  multi("getEntityImmediateChildren", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getString("relationship")
    val a2 = args.getStrings("constrainingTypes")
    (c) => c.getEntityImmediateChildren(a0, a1, a2)
  })
  single("getEntityChildren", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getString("relationship")
    val a2 = args.getInt("depth")
    (c) => c.getEntityChildren(a0, a1, a2)
  })
  single("getEntityChildren", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getString("relationship")
    val a2 = args.getInt("depth")
    val a3 = args.getStrings("constrainingTypes")
    (c) => c.getEntityChildren(a0, a1, a2, a3)
  })
  multi("getEntityChildrenFromTypeRoots", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getString("parentType")
    val a1 = args.getString("relationship")
    val a2 = args.getInt("depth")
    val a3 = args.getStrings("constrainingTypes")
    (c) => c.getEntityChildrenFromTypeRoots(a0, a1, a2, a3)
  })
  multi("getEntityRelationsFromTypeRoots", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getString("parentType")
    val a1 = args.getEntityRelations("relations")
    (c) => c.getEntityRelationsFromTypeRoots(a0, a1)
  })
  multi("getEntityRelations", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuid("parent")
    val a1 = args.getEntityRelations("relations")
    (c) => c.getEntityRelations(a0, a1)
  })
  multi("getEntityRelationsForParents", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getUuids("parentUuids")
    val a1 = args.getEntityRelations("relations")
    (c) => c.getEntityRelationsForParents(a0, a1)
  })
  multi("getEntityRelationsForParentsByName", classOf[org.totalgrid.reef.client.service.proto.Model.Entity], args => {
    val a0 = args.getStrings("parentNames")
    val a1 = args.getEntityRelations("relations")
    (c) => c.getEntityRelationsForParentsByName(a0, a1)
  })
  // Can't encode searchForEntityTree : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
  // Can't encode searchForEntities : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
  single("getEntityAttributes", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getEntityAttributes(a0)
  })
  single("removeEntityAttribute", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    val a1 = args.getString("attrName")
    (c) => c.removeEntityAttribute(a0, a1)
  })
  optional("clearEntityAttributes", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.clearEntityAttributes(a0)
  })
  single("setEntityAttribute", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    val a1 = args.getString("name")
    val a2 = args.getBoolean("value")
    (c) => c.setEntityAttribute(a0, a1, a2)
  })
  single("setEntityAttribute", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    val a1 = args.getString("name")
    val a2 = args.getLong("value")
    (c) => c.setEntityAttribute(a0, a1, a2)
  })
  single("setEntityAttribute", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    val a1 = args.getString("name")
    val a2 = args.getDouble("value")
    (c) => c.setEntityAttribute(a0, a1, a2)
  })
  single("setEntityAttribute", classOf[org.totalgrid.reef.client.service.proto.Model.EntityAttributes], args => {
    val a0 = args.getUuid("uuid")
    val a1 = args.getString("name")
    val a2 = args.getString("value")
    (c) => c.setEntityAttribute(a0, a1, a2)
  })
  // Can't encode setEntityAttribute : Can't encode type: byte[]
  ////////////////////
  // EventConfigService
  ////////////////////
  multi("getEventConfigurations", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => { (c) =>
    c.getEventConfigurations()
  })
  multi("getEventConfigurations", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => {
    val a0 = args.getBoolean("builtIn")
    (c) => c.getEventConfigurations(a0)
  })
  single("getEventConfigurationByType", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => {
    val a0 = args.getString("eventType")
    (c) => c.getEventConfigurationByType(a0)
  })
  single("setEventConfigAsLogOnly", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getInt("severity")
    val a2 = args.getString("resourceString")
    (c) => c.setEventConfigAsLogOnly(a0, a1, a2)
  })
  single("setEventConfigAsEvent", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getInt("severity")
    val a2 = args.getString("resourceString")
    (c) => c.setEventConfigAsEvent(a0, a1, a2)
  })
  single("setEventConfigAsAlarm", classOf[org.totalgrid.reef.client.service.proto.Alarms.EventConfig], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getInt("severity")
    val a2 = args.getString("resourceString")
    val a3 = args.getBoolean("audibleAlarm")
    (c) => c.setEventConfigAsAlarm(a0, a1, a2, a3)
  })
  // Can't encode setEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig.Designation
  // Can't encode deleteEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig
  ////////////////////
  // EventPublishingService
  ////////////////////
  // Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Events.Event
  single("publishEvent", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getString("subsystem")
    (c) => c.publishEvent(a0, a1)
  })
  // Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
  // Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
  single("publishEvent", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getString("subsystem")
    val a2 = args.getUuid("entityUuid")
    (c) => c.publishEvent(a0, a1, a2)
  })
  single("publishEvent", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getString("eventType")
    val a1 = args.getString("subsystem")
    val a2 = args.getLong("deviceTime")
    val a3 = args.getUuid("entityUuid")
    (c) => c.publishEvent(a0, a1, a2, a3)
  })
  // Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
  // Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
  ////////////////////
  // EventService
  ////////////////////
  single("getEventById", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getId("id")
    (c) => c.getEventById(a0)
  })
  multi("getRecentEvents", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getInt("limit")
    (c) => c.getRecentEvents(a0)
  })
  subscription("subscribeToRecentEvents", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getInt("limit")
    (c) => c.subscribeToRecentEvents(a0)
  })
  subscription("subscribeToRecentEvents", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getStrings("types")
    val a1 = args.getInt("limit")
    (c) => c.subscribeToRecentEvents(a0, a1)
  })
  multi("getRecentEvents", classOf[org.totalgrid.reef.client.service.proto.Events.Event], args => {
    val a0 = args.getStrings("types")
    val a1 = args.getInt("limit")
    (c) => c.getRecentEvents(a0, a1)
  })
  // Can't encode searchForEvents : Can't encode type: org.totalgrid.reef.client.service.proto.Events.EventSelect
  // Can't encode subscribeToEvents : Can't encode type: org.totalgrid.reef.client.service.proto.Events.EventSelect
  ////////////////////
  // MeasurementOverrideService
  ////////////////////
  // Can't encode setPointOutOfService : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode setPointOverride : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode deleteMeasurementOverride : Can't encode type: org.totalgrid.reef.client.service.proto.Processing.MeasOverride
  // Can't encode clearMeasurementOverridesOnPoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  ////////////////////
  // MeasurementService
  ////////////////////
  // Can't encode getMeasurementByPoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  single("getMeasurementByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    (c) => c.getMeasurementByName(a0)
  })
  optional("findMeasurementByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    (c) => c.findMeasurementByName(a0)
  })
  multi("getMeasurementsByNames", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getStrings("pointNames")
    (c) => c.getMeasurementsByNames(a0)
  })
  // Can't encode getMeasurementsByPoints : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode subscribeToMeasurementsByPoints : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  subscription("subscribeToMeasurementsByNames", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getStrings("pointNames")
    (c) => c.subscribeToMeasurementsByNames(a0)
  })
  // Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  multi("getMeasurementHistoryByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    val a1 = args.getInt("limit")
    (c) => c.getMeasurementHistoryByName(a0, a1)
  })
  multi("getMeasurementHistoryByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    val a1 = args.getLong("since")
    val a2 = args.getInt("limit")
    (c) => c.getMeasurementHistoryByName(a0, a1, a2)
  })
  multi("getMeasurementHistoryByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    val a1 = args.getLong("from")
    val a2 = args.getLong("to")
    val a3 = args.getBoolean("returnNewest")
    val a4 = args.getInt("limit")
    (c) => c.getMeasurementHistoryByName(a0, a1, a2, a3, a4)
  })
  // Can't encode subscribeToMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  // Can't encode subscribeToMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  subscription("subscribeToMeasurementHistoryByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    val a1 = args.getInt("limit")
    (c) => c.subscribeToMeasurementHistoryByName(a0, a1)
  })
  subscription("subscribeToMeasurementHistoryByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.Measurement], args => {
    val a0 = args.getString("pointName")
    val a1 = args.getLong("since")
    val a2 = args.getInt("limit")
    (c) => c.subscribeToMeasurementHistoryByName(a0, a1, a2)
  })
  // Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
  // Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
  // Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
  // Can't encode getMeasurementStatisticsByPoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
  single("getMeasurementStatisticsByName", classOf[org.totalgrid.reef.client.service.proto.Measurements.MeasurementStatistics], args => {
    val a0 = args.getString("pointName")
    (c) => c.getMeasurementStatisticsByName(a0)
  })
  ////////////////////
  // PointService
  ////////////////////
  multi("getPoints", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => { (c) =>
    c.getPoints()
  })
  single("getPointByName", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getString("name")
    (c) => c.getPointByName(a0)
  })
  optional("findPointByName", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getString("name")
    (c) => c.findPointByName(a0)
  })
  single("getPointByUuid", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getUuid("uuid")
    (c) => c.getPointByUuid(a0)
  })
  multi("getPointsByNames", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getStrings("names")
    (c) => c.getPointsByNames(a0)
  })
  multi("getPointsByUuids", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getUuids("uuids")
    (c) => c.getPointsByUuids(a0)
  })
  // Can't encode getPointsOwnedByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
  multi("getPointsOwnedByEntity", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getUuid("entityUuid")
    (c) => c.getPointsOwnedByEntity(a0)
  })
  multi("getPointsBelongingToEndpoint", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getUuid("endpointUuid")
    (c) => c.getPointsBelongingToEndpoint(a0)
  })
  multi("getPointsThatFeedbackForCommand", classOf[org.totalgrid.reef.client.service.proto.Model.Point], args => {
    val a0 = args.getUuid("commandUuid")
    (c) => c.getPointsThatFeedbackForCommand(a0)
  })
}
