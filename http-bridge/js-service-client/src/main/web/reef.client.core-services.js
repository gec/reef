/*
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
(function($) {
	$.reefServiceList_core = function(client) {
		var calls = {};
		////////////////////
		// AgentService
		////////////////////
		calls.getAgentByName = function(name) {
			return client.apiRequest({
				request: "getAgentByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getAgents = function() {
			return client.apiRequest({
				request: "getAgents",
				style: "MULTI"
			});
		};
		calls.createNewAgent = function(name, password, permissionSetNames) {
			return client.apiRequest({
				request: "createNewAgent",
				data: {
					name: name,
					password: password,
					permissionSetNames: permissionSetNames
				},
				style: "SINGLE"
			});
		};
		// Can't encode deleteAgent : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Agent
		// Can't encode setAgentPassword : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Agent
		calls.getPermissionSets = function() {
			return client.apiRequest({
				request: "getPermissionSets",
				style: "MULTI"
			});
		};
		calls.getPermissionSet = function(name) {
			return client.apiRequest({
				request: "getPermissionSet",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		// Can't encode createPermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Permission
		// Can't encode deletePermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.PermissionSet
		////////////////////
		// AlarmService
		////////////////////
		calls.getAlarmById = function(id) {
			return client.apiRequest({
				request: "getAlarmById",
				data: {
					id: id
				},
				style: "SINGLE"
			});
		};
		calls.getActiveAlarms = function(limit) {
			return client.apiRequest({
				request: "getActiveAlarms",
				data: {
					limit: limit
				},
				style: "MULTI"
			});
		};
		// Can't encode subscribeToActiveAlarms : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Alarms.Alarm>, org.totalgrid.reef.client.service.proto.Alarms.Alarm>
		calls.getActiveAlarms = function(types, recentAlarmLimit) {
			return client.apiRequest({
				request: "getActiveAlarms",
				data: {
					types: types,
					recentAlarmLimit: recentAlarmLimit
				},
				style: "MULTI"
			});
		};
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
		calls.getApplications = function() {
			return client.apiRequest({
				request: "getApplications",
				style: "MULTI"
			});
		};
		calls.findApplicationByName = function(name) {
			return client.apiRequest({
				request: "findApplicationByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getApplicationByName = function(name) {
			return client.apiRequest({
				request: "getApplicationByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getApplicationByUuid = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getApplicationByUuid",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		////////////////////
		// ClientOperations
		////////////////////
		// Can't encode getOne : Can't serialize non-protobuf response: T
		// Can't encode findOne : Can't serialize non-protobuf response: T
		// Can't encode getMany : Can't serialize non-protobuf response: T
		// Can't encode subscribeMany : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<T>, T>
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
		calls.deleteCommandLock = function(commandId) {
			if(commandId.value != undefined) commandId = commandId.value;
			return client.apiRequest({
				request: "deleteCommandLock",
				data: {
					commandId: commandId
				},
				style: "SINGLE"
			});
		};
		calls.clearCommandLocks = function() {
			return client.apiRequest({
				request: "clearCommandLocks",
				style: "MULTI"
			});
		};
		// Can't encode executeCommandAsControl : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		// Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		// Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		// Can't encode executeCommandAsSetpoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		// Can't encode createCommandDenialLock : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		calls.getCommandLocks = function() {
			return client.apiRequest({
				request: "getCommandLocks",
				style: "MULTI"
			});
		};
		calls.getCommandLockById = function(id) {
			if(id.value != undefined) id = id.value;
			return client.apiRequest({
				request: "getCommandLockById",
				data: {
					id: id
				},
				style: "SINGLE"
			});
		};
		// Can't encode findCommandLockOnCommand : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		// Can't encode getCommandLocksOnCommands : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		calls.getCommandHistory = function() {
			return client.apiRequest({
				request: "getCommandHistory",
				style: "MULTI"
			});
		};
		// Can't encode getCommandHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		calls.getCommands = function() {
			return client.apiRequest({
				request: "getCommands",
				style: "MULTI"
			});
		};
		calls.getCommandByName = function(name) {
			return client.apiRequest({
				request: "getCommandByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getCommandByUuid = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getCommandByUuid",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.getCommandsByNames = function(names) {
			return client.apiRequest({
				request: "getCommandsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		calls.getCommandsByUuids = function(uuids) {
			return client.apiRequest({
				request: "getCommandsByUuids",
				data: {
					uuids: uuids
				},
				style: "MULTI"
			});
		};
		calls.getCommandsOwnedByEntity = function(parentUUID) {
			if(parentUUID.value != undefined) parentUUID = parentUUID.value;
			return client.apiRequest({
				request: "getCommandsOwnedByEntity",
				data: {
					parentUUID: parentUUID
				},
				style: "MULTI"
			});
		};
		calls.getCommandsBelongingToEndpoint = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "getCommandsBelongingToEndpoint",
				data: {
					endpointUuid: endpointUuid
				},
				style: "MULTI"
			});
		};
		calls.getCommandsThatFeedbackToPoint = function(pointUuid) {
			if(pointUuid.value != undefined) pointUuid = pointUuid.value;
			return client.apiRequest({
				request: "getCommandsThatFeedbackToPoint",
				data: {
					pointUuid: pointUuid
				},
				style: "MULTI"
			});
		};
		// Can't encode bindCommandHandler : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionBinding
		////////////////////
		// CommunicationChannelService
		////////////////////
		calls.getCommunicationChannels = function() {
			return client.apiRequest({
				request: "getCommunicationChannels",
				style: "MULTI"
			});
		};
		calls.getCommunicationChannelByUuid = function(channelUuid) {
			if(channelUuid.value != undefined) channelUuid = channelUuid.value;
			return client.apiRequest({
				request: "getCommunicationChannelByUuid",
				data: {
					channelUuid: channelUuid
				},
				style: "SINGLE"
			});
		};
		calls.getCommunicationChannelByName = function(channelName) {
			return client.apiRequest({
				request: "getCommunicationChannelByName",
				data: {
					channelName: channelName
				},
				style: "SINGLE"
			});
		};
		// Can't encode alterCommunicationChannelState : Can't encode type: org.totalgrid.reef.client.service.proto.FEP.CommChannel.State
		calls.getEndpointsUsingChannel = function(channelUuid) {
			if(channelUuid.value != undefined) channelUuid = channelUuid.value;
			return client.apiRequest({
				request: "getEndpointsUsingChannel",
				data: {
					channelUuid: channelUuid
				},
				style: "MULTI"
			});
		};
		////////////////////
		// ConfigFileService
		////////////////////
		calls.getConfigFiles = function() {
			return client.apiRequest({
				request: "getConfigFiles",
				style: "MULTI"
			});
		};
		calls.getConfigFileByUuid = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getConfigFileByUuid",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.getConfigFileByName = function(name) {
			return client.apiRequest({
				request: "getConfigFileByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.findConfigFileByName = function(name) {
			return client.apiRequest({
				request: "findConfigFileByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getConfigFilesUsedByEntity = function(entityUuid) {
			if(entityUuid.value != undefined) entityUuid = entityUuid.value;
			return client.apiRequest({
				request: "getConfigFilesUsedByEntity",
				data: {
					entityUuid: entityUuid
				},
				style: "MULTI"
			});
		};
		calls.getConfigFilesUsedByEntity = function(entityUuid, mimeType) {
			if(entityUuid.value != undefined) entityUuid = entityUuid.value;
			return client.apiRequest({
				request: "getConfigFilesUsedByEntity",
				data: {
					entityUuid: entityUuid,
					mimeType: mimeType
				},
				style: "MULTI"
			});
		};
		// Can't encode createConfigFile : Can't encode type: byte[]
		// Can't encode createConfigFile : Can't encode type: byte[]
		// Can't encode createConfigFile : Can't encode type: byte[]
		// Can't encode updateConfigFile : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
		// Can't encode addConfigFileUsedByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
		// Can't encode deleteConfigFile : Can't encode type: org.totalgrid.reef.client.service.proto.Model.ConfigFile
		////////////////////
		// EndpointService
		////////////////////
		calls.getEndpoints = function() {
			return client.apiRequest({
				request: "getEndpoints",
				style: "MULTI"
			});
		};
		calls.getEndpointByName = function(name) {
			return client.apiRequest({
				request: "getEndpointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getEndpointByUuid = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "getEndpointByUuid",
				data: {
					endpointUuid: endpointUuid
				},
				style: "SINGLE"
			});
		};
		calls.getEndpointsByNames = function(names) {
			return client.apiRequest({
				request: "getEndpointsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		calls.getEndpointsByUuids = function(endpointUuids) {
			return client.apiRequest({
				request: "getEndpointsByUuids",
				data: {
					endpointUuids: endpointUuids
				},
				style: "MULTI"
			});
		};
		calls.disableEndpointConnection = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "disableEndpointConnection",
				data: {
					endpointUuid: endpointUuid
				},
				style: "SINGLE"
			});
		};
		calls.enableEndpointConnection = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "enableEndpointConnection",
				data: {
					endpointUuid: endpointUuid
				},
				style: "SINGLE"
			});
		};
		calls.getEndpointConnections = function() {
			return client.apiRequest({
				request: "getEndpointConnections",
				style: "MULTI"
			});
		};
		// Can't encode subscribeToEndpointConnections : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.FEP.EndpointConnection>, org.totalgrid.reef.client.service.proto.FEP.EndpointConnection>
		calls.getEndpointConnectionByUuid = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "getEndpointConnectionByUuid",
				data: {
					endpointUuid: endpointUuid
				},
				style: "SINGLE"
			});
		};
		calls.getEndpointConnectionByEndpointName = function(endpointName) {
			return client.apiRequest({
				request: "getEndpointConnectionByEndpointName",
				data: {
					endpointName: endpointName
				},
				style: "SINGLE"
			});
		};
		// Can't encode alterEndpointConnectionState : Can't encode type: org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State
		////////////////////
		// EntityService
		////////////////////
		calls.getEntities = function() {
			return client.apiRequest({
				request: "getEntities",
				style: "MULTI"
			});
		};
		calls.getEntityByUuid = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getEntityByUuid",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.getEntityByName = function(name) {
			return client.apiRequest({
				request: "getEntityByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getEntitiesByUuids = function(uuids) {
			return client.apiRequest({
				request: "getEntitiesByUuids",
				data: {
					uuids: uuids
				},
				style: "MULTI"
			});
		};
		calls.getEntitiesByNames = function(names) {
			return client.apiRequest({
				request: "getEntitiesByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		calls.findEntityByName = function(name) {
			return client.apiRequest({
				request: "findEntityByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getEntitiesWithType = function(typeName) {
			return client.apiRequest({
				request: "getEntitiesWithType",
				data: {
					typeName: typeName
				},
				style: "MULTI"
			});
		};
		calls.getEntitiesWithTypes = function(types) {
			return client.apiRequest({
				request: "getEntitiesWithTypes",
				data: {
					types: types
				},
				style: "MULTI"
			});
		};
		calls.getEntityRelatedChildrenOfType = function(parent, relationship, typeName) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityRelatedChildrenOfType",
				data: {
					parent: parent,
					relationship: relationship,
					typeName: typeName
				},
				style: "MULTI"
			});
		};
		calls.getEntityImmediateChildren = function(parent, relationship) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityImmediateChildren",
				data: {
					parent: parent,
					relationship: relationship
				},
				style: "MULTI"
			});
		};
		calls.getEntityImmediateChildren = function(parent, relationship, constrainingTypes) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityImmediateChildren",
				data: {
					parent: parent,
					relationship: relationship,
					constrainingTypes: constrainingTypes
				},
				style: "MULTI"
			});
		};
		calls.getEntityChildren = function(parent, relationship, depth) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityChildren",
				data: {
					parent: parent,
					relationship: relationship,
					depth: depth
				},
				style: "SINGLE"
			});
		};
		calls.getEntityChildren = function(parent, relationship, depth, constrainingTypes) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityChildren",
				data: {
					parent: parent,
					relationship: relationship,
					depth: depth,
					constrainingTypes: constrainingTypes
				},
				style: "SINGLE"
			});
		};
		calls.getEntityChildrenFromTypeRoots = function(parentType, relationship, depth, constrainingTypes) {
			return client.apiRequest({
				request: "getEntityChildrenFromTypeRoots",
				data: {
					parentType: parentType,
					relationship: relationship,
					depth: depth,
					constrainingTypes: constrainingTypes
				},
				style: "MULTI"
			});
		};
		// Can't encode getEntityRelationsFromTypeRoots : Can't encode type: org.totalgrid.reef.client.service.entity.EntityRelation
		// Can't encode getEntityRelations : Can't encode type: org.totalgrid.reef.client.service.entity.EntityRelation
		// Can't encode getEntityRelationsForParents : Can't encode type: org.totalgrid.reef.client.service.entity.EntityRelation
		// Can't encode getEntityRelationsForParentsByName : Can't encode type: org.totalgrid.reef.client.service.entity.EntityRelation
		// Can't encode searchForEntityTree : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
		// Can't encode searchForEntities : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
		calls.getEntityAttributes = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getEntityAttributes",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.removeEntityAttribute = function(uuid, attrName) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "removeEntityAttribute",
				data: {
					uuid: uuid,
					attrName: attrName
				},
				style: "SINGLE"
			});
		};
		calls.clearEntityAttributes = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "clearEntityAttributes",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.setEntityAttribute = function(uuid, name, value) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "setEntityAttribute",
				data: {
					uuid: uuid,
					name: name,
					value: value
				},
				style: "SINGLE"
			});
		};
		calls.setEntityAttribute = function(uuid, name, value) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "setEntityAttribute",
				data: {
					uuid: uuid,
					name: name,
					value: value
				},
				style: "SINGLE"
			});
		};
		calls.setEntityAttribute = function(uuid, name, value) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "setEntityAttribute",
				data: {
					uuid: uuid,
					name: name,
					value: value
				},
				style: "SINGLE"
			});
		};
		calls.setEntityAttribute = function(uuid, name, value) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "setEntityAttribute",
				data: {
					uuid: uuid,
					name: name,
					value: value
				},
				style: "SINGLE"
			});
		};
		// Can't encode setEntityAttribute : Can't encode type: byte[]
		////////////////////
		// EventConfigService
		////////////////////
		calls.getEventConfigurations = function() {
			return client.apiRequest({
				request: "getEventConfigurations",
				style: "MULTI"
			});
		};
		calls.getEventConfigurations = function(builtIn) {
			return client.apiRequest({
				request: "getEventConfigurations",
				data: {
					builtIn: builtIn
				},
				style: "MULTI"
			});
		};
		calls.getEventConfigurationByType = function(eventType) {
			return client.apiRequest({
				request: "getEventConfigurationByType",
				data: {
					eventType: eventType
				},
				style: "SINGLE"
			});
		};
		calls.setEventConfigAsLogOnly = function(eventType, severity, resourceString) {
			return client.apiRequest({
				request: "setEventConfigAsLogOnly",
				data: {
					eventType: eventType,
					severity: severity,
					resourceString: resourceString
				},
				style: "SINGLE"
			});
		};
		calls.setEventConfigAsEvent = function(eventType, severity, resourceString) {
			return client.apiRequest({
				request: "setEventConfigAsEvent",
				data: {
					eventType: eventType,
					severity: severity,
					resourceString: resourceString
				},
				style: "SINGLE"
			});
		};
		calls.setEventConfigAsAlarm = function(eventType, severity, resourceString, audibleAlarm) {
			return client.apiRequest({
				request: "setEventConfigAsAlarm",
				data: {
					eventType: eventType,
					severity: severity,
					resourceString: resourceString,
					audibleAlarm: audibleAlarm
				},
				style: "SINGLE"
			});
		};
		// Can't encode setEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig.Designation
		// Can't encode deleteEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig
		////////////////////
		// EventPublishingService
		////////////////////
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Events.Event
		calls.publishEvent = function(eventType, subsystem) {
			return client.apiRequest({
				request: "publishEvent",
				data: {
					eventType: eventType,
					subsystem: subsystem
				},
				style: "SINGLE"
			});
		};
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		calls.publishEvent = function(eventType, subsystem, entityUuid) {
			if(entityUuid.value != undefined) entityUuid = entityUuid.value;
			return client.apiRequest({
				request: "publishEvent",
				data: {
					eventType: eventType,
					subsystem: subsystem,
					entityUuid: entityUuid
				},
				style: "SINGLE"
			});
		};
		calls.publishEvent = function(eventType, subsystem, deviceTime, entityUuid) {
			if(entityUuid.value != undefined) entityUuid = entityUuid.value;
			return client.apiRequest({
				request: "publishEvent",
				data: {
					eventType: eventType,
					subsystem: subsystem,
					deviceTime: deviceTime,
					entityUuid: entityUuid
				},
				style: "SINGLE"
			});
		};
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		////////////////////
		// EventService
		////////////////////
		calls.getEventById = function(id) {
			if(id.value != undefined) id = id.value;
			return client.apiRequest({
				request: "getEventById",
				data: {
					id: id
				},
				style: "SINGLE"
			});
		};
		calls.getRecentEvents = function(limit) {
			return client.apiRequest({
				request: "getRecentEvents",
				data: {
					limit: limit
				},
				style: "MULTI"
			});
		};
		// Can't encode subscribeToRecentEvents : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Events.Event>, org.totalgrid.reef.client.service.proto.Events.Event>
		// Can't encode subscribeToRecentEvents : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Events.Event>, org.totalgrid.reef.client.service.proto.Events.Event>
		calls.getRecentEvents = function(types, limit) {
			return client.apiRequest({
				request: "getRecentEvents",
				data: {
					types: types,
					limit: limit
				},
				style: "MULTI"
			});
		};
		// Can't encode searchForEvents : Can't encode type: org.totalgrid.reef.client.service.proto.Events.EventSelect
		// Can't encode subscribeToEvents : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Events.Event>, org.totalgrid.reef.client.service.proto.Events.Event>
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
		calls.getMeasurementByName = function(pointName) {
			return client.apiRequest({
				request: "getMeasurementByName",
				data: {
					pointName: pointName
				},
				style: "SINGLE"
			});
		};
		calls.findMeasurementByName = function(pointName) {
			return client.apiRequest({
				request: "findMeasurementByName",
				data: {
					pointName: pointName
				},
				style: "SINGLE"
			});
		};
		calls.getMeasurementsByNames = function(pointNames) {
			return client.apiRequest({
				request: "getMeasurementsByNames",
				data: {
					pointNames: pointNames
				},
				style: "MULTI"
			});
		};
		// Can't encode getMeasurementsByPoints : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode subscribeToMeasurementsByPoints : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode subscribeToMeasurementsByNames : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode getMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		calls.getMeasurementHistoryByName = function(pointName, limit) {
			return client.apiRequest({
				request: "getMeasurementHistoryByName",
				data: {
					pointName: pointName,
					limit: limit
				},
				style: "MULTI"
			});
		};
		calls.getMeasurementHistoryByName = function(pointName, since, limit) {
			return client.apiRequest({
				request: "getMeasurementHistoryByName",
				data: {
					pointName: pointName,
					since: since,
					limit: limit
				},
				style: "MULTI"
			});
		};
		calls.getMeasurementHistoryByName = function(pointName, from, to, returnNewest, limit) {
			return client.apiRequest({
				request: "getMeasurementHistoryByName",
				data: {
					pointName: pointName,
					from: from,
					to: to,
					returnNewest: returnNewest,
					limit: limit
				},
				style: "MULTI"
			});
		};
		// Can't encode subscribeToMeasurementHistory : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode subscribeToMeasurementHistory : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode subscribeToMeasurementHistoryByName : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode subscribeToMeasurementHistoryByName : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.Measurements.Measurement>, org.totalgrid.reef.client.service.proto.Measurements.Measurement>
		// Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
		// Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
		// Can't encode publishMeasurements : Can't serialize non-protobuf response: java.lang.Boolean
		// Can't encode getMeasurementStatisticsByPoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		calls.getMeasurementStatisticsByName = function(pointName) {
			return client.apiRequest({
				request: "getMeasurementStatisticsByName",
				data: {
					pointName: pointName
				},
				style: "SINGLE"
			});
		};
		////////////////////
		// PointService
		////////////////////
		calls.getPoints = function() {
			return client.apiRequest({
				request: "getPoints",
				style: "MULTI"
			});
		};
		calls.getPointByName = function(name) {
			return client.apiRequest({
				request: "getPointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.findPointByName = function(name) {
			return client.apiRequest({
				request: "findPointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		calls.getPointByUuid = function(uuid) {
			if(uuid.value != undefined) uuid = uuid.value;
			return client.apiRequest({
				request: "getPointByUuid",
				data: {
					uuid: uuid
				},
				style: "SINGLE"
			});
		};
		calls.getPointsByNames = function(names) {
			return client.apiRequest({
				request: "getPointsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		calls.getPointsByUuids = function(uuids) {
			return client.apiRequest({
				request: "getPointsByUuids",
				data: {
					uuids: uuids
				},
				style: "MULTI"
			});
		};
		// Can't encode getPointsOwnedByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
		calls.getPointsOwnedByEntity = function(entityUuid) {
			if(entityUuid.value != undefined) entityUuid = entityUuid.value;
			return client.apiRequest({
				request: "getPointsOwnedByEntity",
				data: {
					entityUuid: entityUuid
				},
				style: "MULTI"
			});
		};
		calls.getPointsBelongingToEndpoint = function(endpointUuid) {
			if(endpointUuid.value != undefined) endpointUuid = endpointUuid.value;
			return client.apiRequest({
				request: "getPointsBelongingToEndpoint",
				data: {
					endpointUuid: endpointUuid
				},
				style: "MULTI"
			});
		};
		calls.getPointsThatFeedbackForCommand = function(commandUuid) {
			if(commandUuid.value != undefined) commandUuid = commandUuid.value;
			return client.apiRequest({
				request: "getPointsThatFeedbackForCommand",
				data: {
					commandUuid: commandUuid
				},
				style: "MULTI"
			});
		};
		$.extend(client, calls);
	};
})(jQuery);
