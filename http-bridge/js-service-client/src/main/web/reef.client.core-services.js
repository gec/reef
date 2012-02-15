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
		/**
		 * @param name of agent to find
		 * @return the agent requested or throws exception
		*/
		calls.getAgentByName = function(name) {
			return client.apiRequest({
				request: "getAgentByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * @return list of all agents
		*/
		calls.getAgents = function() {
			return client.apiRequest({
				request: "getAgents",
				style: "MULTI"
			});
		};
		/**
		 * Creates (or overwrites) an agent and grants them access to the named PermissionSets
		 *
		 * @param name               agent name
		 * @param password           password for agent, must obey systems password rules
		 * @param permissionSetNames list of permissions sets we want to assign to the user
		 * @return the newly created agent object
		*/
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
		/**
		 * @return list of all of the possible permission sets
		*/
		calls.getPermissionSets = function() {
			return client.apiRequest({
				request: "getPermissionSets",
				style: "MULTI"
			});
		};
		/**
		 * @param name of PermissionSet
		 * @return the permissionset with matching name or an exception is thrown
		*/
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
		/**
		 * Get a single alarm
		 *
		 * @param id id of alarm
		*/
		calls.getAlarmById = function(id) {
			return client.apiRequest({
				request: "getAlarmById",
				data: {
					id: id
				},
				style: "SINGLE"
			});
		};
		/**
		 * Get the most recent alarms
		 *
		 * @param limit the number of incoming alarms
		*/
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
		/**
		 * Get the most recent alarms
		 *
		 * @param types event type names
		 * @param recentAlarmLimit the number of recent alarms
		*/
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
		/**
		 * Gets list of all currently registered applications
		 * @throws ReefServiceException
		*/
		calls.getApplications = function() {
			return client.apiRequest({
				request: "getApplications",
				style: "MULTI"
			});
		};
		/**
		 * find a particular application by name
		 * @param name name of the application
		 * @return application proto or null if not found
		*/
		calls.findApplicationByName = function(name) {
			return client.apiRequest({
				request: "findApplicationByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * retrieve an application by name
		 * @param name name of the application
		 * @return application if found or exception will be thrown
		*/
		calls.getApplicationByName = function(name) {
			return client.apiRequest({
				request: "getApplicationByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * retrieve an application by uuid
		 * @param uuid uuid of the application
		 * @return application if found or exception will be thrown
		*/
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
		/**
		 * Deselect a command or set of commands. When we have completed the execution of a command
		 * we delete the system-wide lock we had. This releases the resource so other agents can
		 * access those commands.
		 * @param commandId
		 * @throws ReefServiceException if an error occurs
		*/
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
		/**
		 * Clear all of the command locks in the system. This is a dangerous operation that should only
		 * be preformed in test environments. In production systems this will fail if any other uses
		 * have locks (since we don't have permission to delete other peoples locks).
		 *
		 * @return the deleted locks
		 * @throws ReefServiceException if an error occurs
		*/
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
		/**
		 * Get a list of all command locks in system
		 * Represents the "access table" for the system. Access entries have one or two
		 * modes, "allowed" and "blocked". Commands cannot be issued unless they have an
		 * "allowed" entry. This "selects" the command for operation by a single user, for
		 * as long as access is held. "Block" allows selects to be disallowed for commands;
		 * meaning no users can access/issue the commands.
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommandLocks = function() {
			return client.apiRequest({
				request: "getCommandLocks",
				style: "MULTI"
			});
		};
		/**
		 * Get a command lock by UUID
		 * @param id the id of the command to lock
		 * @throws ReefServiceException if an error occurs
		*/
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
		/**
		 * Get a recent history of issued commands. Information returned is who issued them and what
		 * the final status was.
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommandHistory = function() {
			return client.apiRequest({
				request: "getCommandHistory",
				style: "MULTI"
			});
		};
		// Can't encode getCommandHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Command
		/**
		 * Get a list of available commands in the system
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommands = function() {
			return client.apiRequest({
				request: "getCommands",
				style: "MULTI"
			});
		};
		/**
		 * Get a command object by name.
		 *
		 * @param name  Command name (example: "substation1.breaker2.trip").
		 * @return command associated with the supplied name
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommandByName = function(name) {
			return client.apiRequest({
				request: "getCommandByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * Get a command object by uuid.
		 *
		 * @param uuid  Entity Uuid
		 * @return command associated with the supplied uuid
		 * @throws ReefServiceException if an error occurs
		*/
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
		/**
		 * Get a command object by name.
		 *
		 * @param names  Command name (example: "substation1.breaker2.trip").
		 * @return command associated with the supplied name
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommandsByNames = function(names) {
			return client.apiRequest({
				request: "getCommandsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		/**
		 * Get a command object by uuid.
		 *
		 * @param uuids  Entity Uuids
		 * @return command associated with the supplied uuid
		 * @throws ReefServiceException if an error occurs
		*/
		calls.getCommandsByUuids = function(uuids) {
			return client.apiRequest({
				request: "getCommandsByUuids",
				data: {
					uuids: uuids
				},
				style: "MULTI"
			});
		};
		/**
		 * retrieve all commands that are have the relationship "owns" to the parent entity
		 *
		 * @param parentUUID uuid of parent entity
		 * @return commands owned by parentEntity
		*/
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
		/**
		 * retrieve all commands that have the relationship "source" to the endpoint
		 *
		 * @param endpointUuid uuid of endpoint
		 * @return all commands that are related to endpoint
		*/
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
		/**
		 * retrieve all commands that have the relationship "feedback" to the point
		 *
		 * @param pointUuid uuid of endpoint
		 * @return all commands that are related to point
		*/
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
		/**
		 *
		 * @return list of all of the communication channels
		*/
		calls.getCommunicationChannels = function() {
			return client.apiRequest({
				request: "getCommunicationChannels",
				style: "MULTI"
			});
		};
		/**
		 * @param channelUuid uuid of channel
		 * @return channel with matching uuid or exception if doesn't exist
		*/
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
		/**
		 * @param channelName name of the channel
		 * @return channel with matching name or exception if doesn't exist
		*/
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
		/**
		 * get a list of all endpoints that use a specific channel
		 * @param channelUuid
		 * @return list of endpoints using this channel
		*/
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
		/**
		 * Get all config files
		*/
		calls.getConfigFiles = function() {
			return client.apiRequest({
				request: "getConfigFiles",
				style: "MULTI"
			});
		};
		/**
		 * retrieve a config file by its UID
		*/
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
		/**
		 * retrieve a config file by its name
		*/
		calls.getConfigFileByName = function(name) {
			return client.apiRequest({
				request: "getConfigFileByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * retrieve a config file by its name
		*/
		calls.findConfigFileByName = function(name) {
			return client.apiRequest({
				request: "findConfigFileByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * search for all config files "used" by an entity
		*/
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
		/**
		 * search for all config files "used" by an entity, only returns files with matching mimeType
		*/
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
		/**
		 * @return list of all endpoints in the system
		*/
		calls.getEndpoints = function() {
			return client.apiRequest({
				request: "getEndpoints",
				style: "MULTI"
			});
		};
		/**
		 * @param name name of endpoint
		 * @return the endpoint with that name or throws an exception
		*/
		calls.getEndpointByName = function(name) {
			return client.apiRequest({
				request: "getEndpointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * @param endpointUuid uuid of endpoint
		 * @return the endpoint with that uuid or throws an exception
		*/
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
		/**
		 * @param names name of endpoint
		 * @return the endpoint with that name or throws an exception
		*/
		calls.getEndpointsByNames = function(names) {
			return client.apiRequest({
				request: "getEndpointsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		/**
		 * @param endpointUuids uuid of endpoint
		 * @return the endpoint with that uuid or throws an exception
		*/
		calls.getEndpointsByUuids = function(endpointUuids) {
			return client.apiRequest({
				request: "getEndpointsByUuids",
				data: {
					endpointUuids: endpointUuids
				},
				style: "MULTI"
			});
		};
		/**
		 * disables automatic protocol adapter assignment and begins stopping any running protocol adapters.
		 * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
		 *
		 * @param endpointUuid uuid of endpoint
		 * @return the connection object representing the current connection state
		*/
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
		/**
		 * enables any automatic protocol adapter assignment and begins starting any available protocol adapters.
		 * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
		 *
		 * @param endpointUuid uuid of endpoint
		 * @return the connection object representing the current connection state
		*/
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
		/**
		 * get all of the objects representing endpoint to protocol adapter connections. Sub protos - Endpoint and frontend
		 * will be filled in with name and uuid
		 *
		 * @return list of all endpoint connection objects
		*/
		calls.getEndpointConnections = function() {
			return client.apiRequest({
				request: "getEndpointConnections",
				style: "MULTI"
			});
		};
		// Can't encode subscribeToEndpointConnections : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionResult<java.util.List<org.totalgrid.reef.client.service.proto.FEP.EndpointConnection>, org.totalgrid.reef.client.service.proto.FEP.EndpointConnection>
		/**
		 * Get current endpoint connection state for an endpoint
		 *
		 * @param endpointUuid uuid of endpoint
		 * @return the connection object representing the current connection state
		*/
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
		/**
		 * Get current endpoint connection state for an endpoint
		 *
		 * @param endpointName name of endpoint
		 * @return the connection object representing the current connection state
		*/
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
		/**
		 * Get all entities, should not be used in large systems
		 *
		 * @return all entities in the system
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntities = function() {
			return client.apiRequest({
				request: "getEntities",
				style: "MULTI"
			});
		};
		/**
		 * Get an entity using its unique identification.
		 *
		 * @param uuid The entity id.
		 * @return The entity object.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Get an entity using its name.
		 *
		 * @param name The configured name of the entity.
		 * @return The entity object.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntityByName = function(name) {
			return client.apiRequest({
				request: "getEntityByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * Get an entity using its unique identification.
		 *
		 * @param uuids The entity id.
		 * @return The entity object.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntitiesByUuids = function(uuids) {
			return client.apiRequest({
				request: "getEntitiesByUuids",
				data: {
					uuids: uuids
				},
				style: "MULTI"
			});
		};
		/**
		 * Get an entity using its name.
		 *
		 * @param names The configured name of the entity.
		 * @return The entity object.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntitiesByNames = function(names) {
			return client.apiRequest({
				request: "getEntitiesByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		/**
		 * Find an entity using its name, returns null if not found
		 *
		 * @param name The configured name of the entity.
		 * @return The entity object or null
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.findEntityByName = function(name) {
			return client.apiRequest({
				request: "findEntityByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * Find all entities with a specified type.
		 *
		 * @param typeName The entity type to search for.
		 * @return The list of entities that have the specified type.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntitiesWithType = function(typeName) {
			return client.apiRequest({
				request: "getEntitiesWithType",
				data: {
					typeName: typeName
				},
				style: "MULTI"
			});
		};
		/**
		 * Find all entities matching at least one of the specified types.
		 *
		 * @param types List of entity types to search for. An entity matches if it has at least one of the specified types.
		 * @return The list of entities that have the specified types.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntitiesWithTypes = function(types) {
			return client.apiRequest({
				request: "getEntitiesWithTypes",
				data: {
					types: types
				},
				style: "MULTI"
			});
		};
		/**
		 * Return all child entities that have the correct type and a matching
		 * relationship to the specified parent Entity. The results
		 * are "flattened" and all children are returned in one list so any
		 * relationships or groupings of the child entities will be discarded.
		 *
		 * @param parent       a reference to the parent entity on which to root the request
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @param typeName     the "type" or "class" the matching children need to have
		 * @return list of all children in arbitrary order
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Return direct children of the parent Entity (distance of 1). Just children are returned.
		 * @param parent       a reference to the parent entity on which to root the request
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @return  list of all children in arbitrary order
		 * @throws ReefServiceException
		*/
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
		/**
		 * Return direct children of the parent Entity (distance of 1). Just children are returned.
		 * @param parent       a reference to the parent entity on which to root the request
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
		 *                          of the indicated types are returned
		 * @return  list of all children in arbitrary order
		 * @throws ReefServiceException
		*/
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
		/**
		 * Return a tree of upto depth with all nodes related to each other
		 * @param parent       a reference to the parent entity on which to root the request
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @param depth        how many plies deep we want to
		 * @return  the root entity filled out with children
		 * @throws ReefServiceException
		*/
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
		/**
		 * Return a tree of upto depth with all nodes in constraining types related to each other
		 * @param parent       a reference to the parent entity on which to root the request
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @param depth        how many plies deep we want to
		 * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
		 *                          of the indicated types are returned
		 * @return  the root entity filled out with children
		 * @throws ReefServiceException
		*/
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
		/**
		 * Return a tree of upto depth with all nodes in constraining types related to each other
		 * @param parentType   a type for all of the roots we want to use ("Root")
		 * @param relationship the "color" of the edge between the parent and child, common ones are "owns", "source", "feedback
		 * @param depth        how many plies deep we want to
		 * @param constrainingTypes list of children types we would like to returned, only those children that have atleast one
		 *                          of the indicated types are returned
		 * @return  the root entity filled out with children
		 * @throws ReefServiceException
		*/
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
		/**
		 * Get all attributes associated with a specified Entity.
		 *
		 * @param uuid The entity uuid.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Remove a specific attribute by name for a specified Entity.
		 *
		 * @param uuid      The entity uuid.
		 * @param attrName The name of the attribute.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Clear all attributes for a specified Entity.
		 *
		 * @param uuid The entity uuid.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Set a boolean attribute by name for a specified Entity.
		 *
		 * @param uuid   The entity uuid.
		 * @param name  The name of the attribute.
		 * @param value The attribute value.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Set a signed 64-bit integer attribute by name for a specified Entity.
		 *
		 * @param uuid   The entity uuid.
		 * @param name  The name of the attribute.
		 * @param value The attribute value.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Set a double attribute by name for a specified Entity.
		 *
		 * @param uuid   The entity uuid.
		 * @param name  The name of the attribute.
		 * @param value The attribute value.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * Set a string attribute by name for a specified Entity.
		 *
		 * @param uuid   The entity uuid.
		 * @param name  The name of the attribute.
		 * @param value The attribute value.
		 * @return The entity and its associated attributes.
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
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
		/**
		 * get all of the event handling configurations
		*/
		calls.getEventConfigurations = function() {
			return client.apiRequest({
				request: "getEventConfigurations",
				style: "MULTI"
			});
		};
		/**
		 * @param builtIn event configurations fall into two categories, either builtIn or custom.
		 *                users can only delete custom configurations
		 * @return get a subset of the event configurations
		*/
		calls.getEventConfigurations = function(builtIn) {
			return client.apiRequest({
				request: "getEventConfigurations",
				data: {
					builtIn: builtIn
				},
				style: "MULTI"
			});
		};
		/**
		 * get a single event handling configuration or throw an exception it doesn't exist
		 * @param eventType get a single
		*/
		calls.getEventConfigurationByType = function(eventType) {
			return client.apiRequest({
				request: "getEventConfigurationByType",
				data: {
					eventType: eventType
				},
				style: "SINGLE"
			});
		};
		/**
		 * Create a new event routing configuration that routes only to log file
		 * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
		 * @param severity severity to attach to event
		 * @param resourceString format string to render while replacing the named attributes
		 * @return newly generated event routing configuration
		*/
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
		/**
		 * Create a new event routing configuration that routes to event table and log file
		 * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
		 * @param severity severity to attach to event
		 * @param resourceString format string to render while replacing the named attributes
		 * @return newly generated event routing configuration
		*/
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
		/**
		 * Create a new event routing configuration that routes to event table and log file and makes an alarm lifecycle object
		 * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
		 * @param severity severity to attach to event
		 * @param resourceString format string to render while replacing the named attributes
		 * @param audibleAlarm should alarm start out in the UNACK_AUDIBLE state, making noise on operator consoles
		 * @return newly generated event routing configuration
		*/
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
		/**
		 * publish the simplest type of event which has no interesting details
		 * @param eventType string name of the event type, must match an EventConfig entry
		 * @param subsystem name of subsystem instance that generated message
		 * @return created Event
		*/
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
		/**
		 * publish an event that should be linked to a particular entity
		 * @param eventType string name of the event type, must match an EventConfig entry
		 * @param subsystem name of subsystem instance that generated message
		 * @param entityUuid uuid of the entity most closely related to this event
		 * @return created Event
		*/
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
		/**
		 * publish an event that should be linked to a particular entity
		 * @param eventType string name of the event type, must match an EventConfig entry
		 * @param subsystem name of subsystem instance that generated message
		 * @param deviceTime time in millis when the event should be regarded as occurring
		 * @param entityUuid uuid of the entity most closely related to this event
		 * @return created Event
		*/
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
		/**
		 * get a single event
		 *
		 * @param id event
		*/
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
		/**
		 * get the most recent events
		 *
		 * @param limit the number of incoming events
		*/
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
		/**
		 * get the most recent events
		 *
		 * @param types event type names
		 * @param limit the number of incoming events
		*/
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
		/**
		 * Get the most recent measurement for a point.
		*/
		calls.getMeasurementByName = function(pointName) {
			return client.apiRequest({
				request: "getMeasurementByName",
				data: {
					pointName: pointName
				},
				style: "SINGLE"
			});
		};
		/**
		 * Find the most recent measurement for a point, returning null if the measurement is unknown
		*/
		calls.findMeasurementByName = function(pointName) {
			return client.apiRequest({
				request: "findMeasurementByName",
				data: {
					pointName: pointName
				},
				style: "SINGLE"
			});
		};
		/**
		 * Get the most recent measurement for a set of points. If any points are unknown,
		 * the call will throw a bad request exception.
		*/
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
		/**
		 * Get a list of recent measurements for a point.
		 *
		 * @param limit  Max number of measurements returned
		*/
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
		/**
		 * Get a list of historical measurements that were recorded on or after the specified time.
		 *
		 * @param since  Return measurements on or after this date/time (in milliseconds).
		 * @param limit  max number of measurements returned
		*/
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
		/**
		 * Get a list of historical measurements for the specified time span.
		 *
		 * @param from         Return measurements on or after this time (milliseconds)
		 * @param to           Return measurements on or before this time (milliseconds)
		 * @param returnNewest If there are more measurements than the specified limit, return the newest (true) or oldest (false).
		 * @param limit        Max number of measurements returned
		*/
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
		/**
		 * returns statistics on the point including oldest measurement, and total count
		 * @return measurement statistics proto
		*/
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
		/**
		 * get all points in the system
		 *
		 * @return all points
		*/
		calls.getPoints = function() {
			return client.apiRequest({
				request: "getPoints",
				style: "MULTI"
			});
		};
		/**
		 * retrieve a point by name, throws exception if point is unknown
		 *
		 * @param name of the Point we are retrieving
		 * @return the point object with matching name
		*/
		calls.getPointByName = function(name) {
			return client.apiRequest({
				request: "getPointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * retrieve a point by name, throws exception if point is unknown
		 *
		 * @param name of the Point we are retrieving
		 * @return the point object with matching name
		*/
		calls.findPointByName = function(name) {
			return client.apiRequest({
				request: "findPointByName",
				data: {
					name: name
				},
				style: "SINGLE"
			});
		};
		/**
		 * retrieve a point by uuid, throws exception if point is unknown
		 *
		 * @param uuid of the Point we are retrieving
		 * @return the point object with matching name
		*/
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
		/**
		 * retrieve a point by name, throws exception if point is unknown
		 *
		 * @param names of the Point we are retrieving
		 * @return the point object with matching name
		*/
		calls.getPointsByNames = function(names) {
			return client.apiRequest({
				request: "getPointsByNames",
				data: {
					names: names
				},
				style: "MULTI"
			});
		};
		/**
		 * retrieve a point by uuid, throws exception if point is unknown
		 *
		 * @param uuids of the Point we are retrieving
		 * @return the point object with matching name
		*/
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
		/**
		 * retrieve all points that are have the relationship "owns" to the parent entity
		 *
		 * @param entityUuid parents Uuid
		 * @return points owned by parentEntity
		*/
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
		/**
		 * retrieve all points that are have the relationship "source" to the endpoint
		 *
		 * @param endpointUuid uuid of endpoint
		 * @return all points that are related to endpoint
		*/
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
		/**
		 * retrieve all points that are have the relationship "feeback" to the command
		 *
		 * @param commandUuid uuid of endpoint
		 * @return all points that are related to command
		*/
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
