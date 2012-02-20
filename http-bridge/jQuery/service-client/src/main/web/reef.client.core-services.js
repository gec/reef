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
		 * A service interface for managing and retrieving Agents. An Agent has a name,
		 * password, and a set of permissions in the Reef system. An Agent can be a
		 * real user that logs into the system or a software service that "owns" an
		 * agent it uses to access to other services.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "agent"
			});
		};
		/**
		 * @return list of all agents
		*/
		calls.getAgents = function() {
			return client.apiRequest({
				request: "getAgents",
				style: "MULTI",
				resultType: "agent"
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
				style: "SINGLE",
				resultType: "agent"
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
				style: "MULTI",
				resultType: "permission_set"
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
				style: "SINGLE",
				resultType: "permission_set"
			});
		};
		// Can't encode createPermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.Permission
		// Can't encode deletePermissionSet : Can't encode type: org.totalgrid.reef.client.service.proto.Auth.PermissionSet
		////////////////////
		// AlarmService
		////////////////////
		/**
		 * A service interface for managing and retrieving Alarms. Alarms are special
		 * system events that require operator intervention. Each alarm has an
		 * associated event object, but not all events are alarms.
		 * <p/>
		 * In contrast to events, alarms have persistent state. The three principal alarm states are unacknowledged,
		 * acknowledged, and removed. The transitions between these states constitute the alarm lifecycle, and
		 * manipulation of the states involves user workflow.
		 * <p/>
		 * Transitions in alarm state may themselves be events, as they are part of the record of user operations.
		 * <p/>
		 * During the configuration process, the system designer decides what events trigger alarms. The primary consumers of
		 * alarms are operators tasked with monitoring the system in real-time and responding to abnormal conditions.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "alarm"
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
				style: "MULTI",
				resultType: "alarm"
			});
		};
		/**
		 * Get the most recent alarms and setup a subscription to all future alarms
		 *
		 * @param recentAlarmLimit the number of recent alarms.
		*/
		calls.subscribeToActiveAlarms = function(recentAlarmLimit) {
			return client.subscribeApiRequest({
				request: "subscribeToActiveAlarms",
				data: {
					recentAlarmLimit: recentAlarmLimit
				},
				style: "MULTI",
				resultType: "alarm"
			});
		};
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
				style: "MULTI",
				resultType: "alarm"
			});
		};
		// Can't encode getActiveAlarmsByEntity : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Entity
		// Can't encode silenceAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
		// Can't encode acknowledgeAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
		// Can't encode removeAlarm : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.Alarm
		////////////////////
		// ApplicationService
		////////////////////
		/**
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "MULTI",
				resultType: "application_config"
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
				style: "OPTIONAL",
				resultType: "application_config"
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
				style: "SINGLE",
				resultType: "application_config"
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
				style: "SINGLE",
				resultType: "application_config"
			});
		};
		////////////////////
		// ClientOperations
		////////////////////
		/**
		 * All of the calls to the reef server are implemented by using one of the 4 verbs (GET, PUT, DELETE, POST)
		 * and a protobuf object that serves as a request. The service APIs provided cover 95% of the use cases we
		 * expect applications to use but if a particular query is missing from the API we want to provide a way to
		 * send a custom query to the server. This allows a quick way for an extra request to be implemented for one
		 * application with needing to wait for an update to this package.
		 *
		 * Most clients will not need to use this interface so it has intentionally been left out of the big "rollup"
		 * traits. This interface can be thought of as the "low-level" interface to reef, an application could be
		 * constructed entirely using these sorts of queries but it would be much harder than using the semantic APIs.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
		/**
		 *
		 * <p>
		 *   Service for issuing commands and controls on field devices.</p>
		 *
		 * <h3>Overview</h3>
		 *
		 * <p>SCADA systems use commands to affect changes in the field devices. Commands are usually executed in the field
		 * by the same equipment that is generating measurements. Each command usually represents one action that can
		 * be taken in the field like tripping a breaker or raising the setpoint voltage.
		 *
		 * <h3>Command</h3>
		 * <p>
		 *   The term "command" is a specific command on a specific field device instance. A command "name" is the
		 *   specific command for a device appended to the device name (ex: "substation1.breaker2.trip").</p>
		 *
		 * <h3>Select/Lock a Command</h3>
		 * <p>
		 *   An agent cannot execute a command until they first "select" the command to acquire an exclusive
		 *   lock on the command. The terms "select" and "lock" can be used interchangeably. The exclusive lock
		 *   is tied to the agent who acquired them and do not need to be passed with the command execute. The
		 *   agent may execute the command from different login session (ex: two browser windows).</p>
		 *
		 * <ul>
		 *   <li>A select is designed to be held for seconds up to minutes after which time it is deselected automatically.</li>
		 *   <li>The agent whom created the select should release the select.</li>
		 *   <li>A select is not automatically released after a command is executed.</li>
		 *   <li>Command Denial locks do not timeout (see below).</li>
		 * </ul>
		 *
		 * <h4>Usage</h4>
		 * <p>Issue a command: select, execute, deselect.</p>
		 * <pre>
		 *    Command cmd = getCommandByName( "Substation1.Breaker2.Trip");
		 *    CommandLock lock = createCommandExecutionLock( cmd);
		 *    executeCommandAsControl( cmd);
		 *    deleteCommandLock( lock);
		 * </pre>
		 *
		 * <p>Operate all commands on a piece of equipment using one lock.</p>
		 * <pre>
		 *    List<Command> cmds = getCommandsOwnedByEntity( "Substation1.Breaker2");
		 *    CommandLock lock = createCommandExecutionLock( cmds);
		 *    for(Command c : cmds){
		 *        executeCommandAsControl( c);
		 *    }
		 *    deleteCommandLock( lock);
		 * </pre>
		 *
		 * <h3>Command Denial Lock</h3>
		 * <p>
		 *   when an operator needs to make sure no one will execute any of a set of commands they
		 *   create a system-wide "denial lock" on those commands. This will prevent all operators and
		 *   applications from issuing a command or selecting those commands. To
		 *   execute those commands the lock will need to be deleted.</p>
		 *
		 * <p>
		 *   By default, Denial locks do not timeout like "execution locks".</p>
		 *
		 *
		 *  Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "command_lock"
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
				style: "MULTI",
				resultType: "command_lock"
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
				style: "MULTI",
				resultType: "command_lock"
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
				style: "SINGLE",
				resultType: "command_lock"
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
				style: "MULTI",
				resultType: "user_command_request"
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
				style: "MULTI",
				resultType: "command"
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
				style: "SINGLE",
				resultType: "command"
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
				style: "SINGLE",
				resultType: "command"
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
				style: "MULTI",
				resultType: "command"
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
				style: "MULTI",
				resultType: "command"
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
				style: "MULTI",
				resultType: "command"
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
				style: "MULTI",
				resultType: "command"
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
				style: "MULTI",
				resultType: "command"
			});
		};
		// Can't encode bindCommandHandler : Can't serialize non-protobuf response: org.totalgrid.reef.client.SubscriptionBinding
		////////////////////
		// CommunicationChannelService
		////////////////////
		/**
		 * In reef a communication channel is the representation of the "low-level" connection to an external resource
		 * like a serial port or tcp socket.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		/**
		 *
		 * @return list of all of the communication channels
		*/
		calls.getCommunicationChannels = function() {
			return client.apiRequest({
				request: "getCommunicationChannels",
				style: "MULTI",
				resultType: "comm_channel"
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
				style: "SINGLE",
				resultType: "comm_channel"
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
				style: "SINGLE",
				resultType: "comm_channel"
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
				style: "MULTI",
				resultType: "endpoint"
			});
		};
		////////////////////
		// ConfigFileService
		////////////////////
		/**
		 * Non-exhaustive API for using the reef Config File service, not all valid permutations are reflected here.
		 * Additional functions are expected to be added by clients who extends this interface and add the needed
		 * functionality using ConfigFileServiceImpl as a examples of other valid queries. Note that this class is a
		 * simple interface so it should be easily mockable in test code. Note also that when are using Lists etc. we
		 * are using the java classes instead of scala versions b/c its easier to use java lists in scala than scala
		 * lists in java.
		 * <p/>
		 * Config files are for larger hunks of opaque data for use by external applications. Config files can be
		 * used by 0, 1 or many entities. Config files can be searched for by name, id or by entities they are
		 * related to. Names must be unique system-wide. Searches can all be filtered by mimeType, which can be
		 * helpful is name is unknown.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		/**
		 * Get all config files
		*/
		calls.getConfigFiles = function() {
			return client.apiRequest({
				request: "getConfigFiles",
				style: "MULTI",
				resultType: "config_file"
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
				style: "SINGLE",
				resultType: "config_file"
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
				style: "SINGLE",
				resultType: "config_file"
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
				style: "OPTIONAL",
				resultType: "config_file"
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
				style: "MULTI",
				resultType: "config_file"
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
				style: "MULTI",
				resultType: "config_file"
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
		 * Communication Endpoints are the "field devices" that reef communicates with using legacy protocols
		 * to acquire measurements from the field. Every point and command in the system is associated with
		 * at most one endpoint at a time. Endpoint includes information about the protocol, associated
		 * points, associated commands, communication channels, config files.
		 * <p/>
		 * For protocols that have reef front-end support there is an auxiliary service associated with Endpoints that
		 * tracks which front-end each endpoint is assigned to. It also tracks the current state of the legacy protocol
		 * connection which is how the protocol adapters tell reef if they are successfully communicating with the field
		 * devices. We can also disable (and re-enable) the endpoint connection attempts, this is useful for devices that
		 * can only talk with one "master" at a time so we can disable reefs protocol adapters temporarily to allow
		 * another master to connect.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		/**
		 * @return list of all endpoints in the system
		*/
		calls.getEndpoints = function() {
			return client.apiRequest({
				request: "getEndpoints",
				style: "MULTI",
				resultType: "endpoint"
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
				style: "SINGLE",
				resultType: "endpoint"
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
				style: "SINGLE",
				resultType: "endpoint"
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
				style: "MULTI",
				resultType: "endpoint"
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
				style: "MULTI",
				resultType: "endpoint"
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
				style: "SINGLE",
				resultType: "endpoint_connection"
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
				style: "SINGLE",
				resultType: "endpoint_connection"
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
				style: "MULTI",
				resultType: "endpoint_connection"
			});
		};
		/**
		 * Same as getEndpointConnections but subscribes the user to all changes
		 *
		 * @return list of all endpoint connection objects
		 * @see #getEndpointConnections()
		*/
		calls.subscribeToEndpointConnections = function() {
			return client.subscribeApiRequest({
				request: "subscribeToEndpointConnections",
				style: "MULTI",
				resultType: "endpoint_connection"
			});
		};
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
				style: "SINGLE",
				resultType: "endpoint_connection"
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
				style: "SINGLE",
				resultType: "endpoint_connection"
			});
		};
		// Can't encode alterEndpointConnectionState : Can't encode type: org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State
		////////////////////
		// EntityService
		////////////////////
		/**
		 * <p>
		 *   Service for retrieving entity relationships and storing/retrieving entity attributes.</p>
		 *
		 * <p>
		 *   Note: Many terms for this section are take from <a href="http://en.wikipedia.org/wiki/Graph_theory">graph theory</a>.</p>
		 *
		 * <p>
		 *   The EntityService provides access to the system model. The model is both the pool of
		 *   "entities" and the relationships (edges) connecting those entities to each other.
		 *   Entity types include: Agent, Substations, EquipmentGroup, Breaker, Point, etc.
		 *   Two entities may be related by more than one type of relationship (examples: owns,
		 *   feedback, etc.)</p>
		 *
		 * <p>
		 *   The specific entities and relationships modeled in a particular reef installation
		 *   depends on what the system is being used for (ex: SCADA, Hydro, Microgrid, etc.).</p>
		 *
		 * <p>
		 *   Many of the entities in the "entity pool" have more specific type information beyond Entity.
		 *   An example is Point. There is an Entity representation of a point that is used to
		 *   describe logical relationships to equipment and commands. At the same time, there is a Point
		 *   representation available through the PointService that includes added data like whether that
		 *   point is currently abnormal etc. Clients are expected to use the more specific services for
		 *   basic relationship queries and to retrieve the detailed information available for each specific type. The
		 *   EntityService is available for more complex queries that select objects based on the the many
		 *   relationships between entities. Once these entities are returned, the client can use the
		 *   type specific services to get more type-specific information.</p>
		 *
		 * <p>
		 *   Examples of relationship types (colors)  are:</p>
		 *   <ul>
		 *     <li>owns - used in power systems to model how points and commands are logically considered to be parts of equipment</li>
		 *     <li>feedback - denotes which Points are affected by which Commands</li>
		 *     <li>source - denotes the data provider for a Point or Command (communication pathway)</li>
		 *   </ul>
		 *
		 * <p>
		 * In each installation of Reef there are a further set of constraints applied over this basic model that make the model
		 * easier to consume and reason about, there should be accompanying documentation that describe what those constraints
		 * are. In a future release those constraints will themselves be queryable so applications can be more self configuring,
		 * Currently the developer needs to have a decent idea as to the model to construct a useful query.</p>
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		/**
		 * Get all entities, should not be used in large systems
		 *
		 * @return all entities in the system
		 * @throws org.totalgrid.reef.client.exception.ReefServiceException
		*/
		calls.getEntities = function() {
			return client.apiRequest({
				request: "getEntities",
				style: "MULTI",
				resultType: "entity"
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
				style: "SINGLE",
				resultType: "entity"
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
				style: "SINGLE",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "OPTIONAL",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
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
				style: "SINGLE",
				resultType: "entity"
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
				style: "SINGLE",
				resultType: "entity"
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
				style: "MULTI",
				resultType: "entity"
			});
		};
		/**
		 * Collect a more interesting tree structure
		 * @param parentType   a type for all of the roots we want to use ("Root")
		 * @param relations    list of relations we want to use, first entry in the list is relations to
		 * @return  list of entities with of ParentType and their relations below them
		 * @throws ReefServiceException
		*/
		calls.getEntityRelationsFromTypeRoots = function(parentType, relations) {
			return client.apiRequest({
				request: "getEntityRelationsFromTypeRoots",
				data: {
					parentType: parentType,
					relations: relations
				},
				style: "MULTI",
				resultType: "entity"
			});
		};
		/**
		 * Return a tree of upto depth with all nodes in constraining types related to each other.
		 * @param parent   UUID for parent we want children for
		 * @param relations list of relationship types
		 * @return  list of first level child entities any children they have are contained in the Entity.getRelatations
		 * @throws ReefServiceException
		*/
		calls.getEntityRelations = function(parent, relations) {
			if(parent.value != undefined) parent = parent.value;
			return client.apiRequest({
				request: "getEntityRelations",
				data: {
					parent: parent,
					relations: relations
				},
				style: "MULTI",
				resultType: "entity"
			});
		};
		/**
		 * Collect a more interesting tree structure from a list of specific parent nodes
		 * @param parentUuids  list of specific parents we want relations of
		 * @param relations    list of relations we want to use, first entry in the list is relations to
		 * @return  list of entities with of ParentType and their relations below them
		 * @throws ReefServiceException
		*/
		calls.getEntityRelationsForParents = function(parentUuids, relations) {
			return client.apiRequest({
				request: "getEntityRelationsForParents",
				data: {
					parentUuids: parentUuids,
					relations: relations
				},
				style: "MULTI",
				resultType: "entity"
			});
		};
		/**
		 * Collect a more interesting tree structure from a list of specific parent nodes
		 * @param parentNames  list of specific parents we want relations of
		 * @param relations    list of relations we want to use, first entry in the list is relations to
		 * @return  list of entities with of ParentType and their relations below them
		 * @throws ReefServiceException
		*/
		calls.getEntityRelationsForParentsByName = function(parentNames, relations) {
			return client.apiRequest({
				request: "getEntityRelationsForParentsByName",
				data: {
					parentNames: parentNames,
					relations: relations
				},
				style: "MULTI",
				resultType: "entity"
			});
		};
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
				style: "SINGLE",
				resultType: "entity_attributes"
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
				style: "SINGLE",
				resultType: "entity_attributes"
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
				style: "OPTIONAL",
				resultType: "entity_attributes"
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
				style: "SINGLE",
				resultType: "entity_attributes"
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
				style: "SINGLE",
				resultType: "entity_attributes"
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
				style: "SINGLE",
				resultType: "entity_attributes"
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
				style: "SINGLE",
				resultType: "entity_attributes"
			});
		};
		// Can't encode setEntityAttribute : Can't encode type: byte[]
		////////////////////
		// EventConfigService
		////////////////////
		/**
		 * Event Configs describe all of the types of "well known" event types in the system. When an event occurs,
		 * 1 of 3 things will occur:
		 *
		 * <ol>
		 * <li>
		 * <b>Log:</b>
		 * The event will be logged to a system file but not stored in the events table. This is
		 * primarily used when we want to suppress events that are deemed unimportant in this installation.
		 * </li>
		 * <li>
		 * <b>Event:</b>
		 * The event will be stored in the events table. This will generally be visibile on an HMI
		 * but usually wouldn't be cause a "push" notification to users.
		 * </li>
		 * <li>
		 * <b>Alarm:</b>
		 * If an event is configured to have type Alarm it is recorded as an event but an alarm
		 * notification is also generated. This is usually pushed to the users quickly since a user needs to interact
		 * with the system to silence and/or acknowledge the alarm.
		 * </li>
		 * </ol>
		 *
		 *  Tag for api-enhancer, do not delete: 
		*/
		/**
		 * get all of the event handling configurations
		*/
		calls.getEventConfigurations = function() {
			return client.apiRequest({
				request: "getEventConfigurations",
				style: "MULTI",
				resultType: "event_config"
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
				style: "MULTI",
				resultType: "event_config"
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
				style: "SINGLE",
				resultType: "event_config"
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
				style: "SINGLE",
				resultType: "event_config"
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
				style: "SINGLE",
				resultType: "event_config"
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
				style: "SINGLE",
				resultType: "event_config"
			});
		};
		// Can't encode setEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig.Designation
		// Can't encode deleteEventConfig : Can't encode type: org.totalgrid.reef.client.service.proto.Alarms.EventConfig
		////////////////////
		// EventPublishingService
		////////////////////
		/**
		 * When an application wants to publish an Event they should only supply the interesting
		 * information they have on hand. The server will set most of the fields when the event is posted
		 * including:
		 *  - user_id that created an event (based on the posters auth token)
		 *  - time when the event occured (actually records when the event service processes the put, if a very specific
		 *    time is desired use device_time)
		 *  - severity, rendered, alarm fields are all set based on the matching EventConfig record
		 *  - id is the Event id, if the returned event doesn't have this field set it means it was logged or dropped
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "event"
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
				style: "SINGLE",
				resultType: "event"
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
				style: "SINGLE",
				resultType: "event"
			});
		};
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		// Can't encode publishEvent : Can't encode type: org.totalgrid.reef.client.service.proto.Utils.Attribute
		////////////////////
		// EventService
		////////////////////
		/**
		 * This service is used to get and produce Events on the reef system. Events are generated by the system in response
		 * to unusual or interesting occurances, usually they are interesting to an operator but do not require immediate action.
		 * When an event is published the system may "upgrade" an Event to also generate an alarm.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "event"
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
				style: "MULTI",
				resultType: "event"
			});
		};
		/**
		 * get the most recent events and setup a subscription to all future events
		 *
		 * @param limit the number of incoming events
		*/
		calls.subscribeToRecentEvents = function(limit) {
			return client.subscribeApiRequest({
				request: "subscribeToRecentEvents",
				data: {
					limit: limit
				},
				style: "MULTI",
				resultType: "event"
			});
		};
		/**
		 * get the most recent events and setup a subscription to all future events
		 *
		 * @param types event type names
		 * @param limit the number of incoming events
		*/
		calls.subscribeToRecentEvents = function(types, limit) {
			return client.subscribeApiRequest({
				request: "subscribeToRecentEvents",
				data: {
					types: types,
					limit: limit
				},
				style: "MULTI",
				resultType: "event"
			});
		};
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
				style: "MULTI",
				resultType: "event"
			});
		};
		// Can't encode searchForEvents : Can't encode type: org.totalgrid.reef.client.service.proto.Events.EventSelect
		// Can't encode subscribeToEvents : Can't encode type: org.totalgrid.reef.client.service.proto.Events.EventSelect
		////////////////////
		// MeasurementOverrideService
		////////////////////
		/**
		 * SCADA systems there is the concept of a stopping the measurement stream from field and publishing another value in
		 * its place. There are 2 ways this is done, marking a point "Not in Service" (NIS) and "Overriding" the point.
		 * <p/>
		 * This is usually done for one of two  reasons*  - A field devices is reporting a bad value (wildly oscillating or pinned to 0) and is generating spurious
		 * alarms or confusing "advanced apps" that are performing calculations on that value. In this case an operator will
		 * override that data to its nominal value.
		 * - Training/Testing purposes, when an operator or integrator is testing alarms/UI/apps its often valuable to just
		 * be able to quickly override a value and see that the correct behaviors occur.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		// Can't encode setPointOutOfService : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode setPointOverride : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode deleteMeasurementOverride : Can't encode type: org.totalgrid.reef.client.service.proto.Processing.MeasOverride
		// Can't encode clearMeasurementOverridesOnPoint : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		////////////////////
		// MeasurementService
		////////////////////
		/**
		 * <p>
		 *   Service for retrieving, subscribing to, and publishing measurements. Clients can retrieve current measurement
		 *   values for multiple points, read historical values for a single point, or
		 *   publish measurements in batches.
		 *   </p>
		 *
		 * <p>
		 *   Asking for unknown points will result in an exception.
		 *   </p>
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
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
				style: "SINGLE",
				resultType: "measurement"
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
				style: "OPTIONAL",
				resultType: "measurement"
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
				style: "MULTI",
				resultType: "measurement"
			});
		};
		// Can't encode getMeasurementsByPoints : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode subscribeToMeasurementsByPoints : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		/**
		 * Gets the most recent measurement for a set of points and subscribe to receive updates for
		 * measurement changes.
		*/
		calls.subscribeToMeasurementsByNames = function(pointNames) {
			return client.subscribeApiRequest({
				request: "subscribeToMeasurementsByNames",
				data: {
					pointNames: pointNames
				},
				style: "MULTI",
				resultType: "measurement"
			});
		};
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
				style: "MULTI",
				resultType: "measurement"
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
				style: "MULTI",
				resultType: "measurement"
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
				style: "MULTI",
				resultType: "measurement"
			});
		};
		// Can't encode subscribeToMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		// Can't encode subscribeToMeasurementHistory : Can't encode type: org.totalgrid.reef.client.service.proto.Model.Point
		/**
		 * Get the most recent measurements for a point and subscribe to receive updates for
		 * measurement changes.
		 *
		 * @param limit  Max number of measurements returned
		*/
		calls.subscribeToMeasurementHistoryByName = function(pointName, limit) {
			return client.subscribeApiRequest({
				request: "subscribeToMeasurementHistoryByName",
				data: {
					pointName: pointName,
					limit: limit
				},
				style: "MULTI",
				resultType: "measurement"
			});
		};
		/**
		 * Get the most recent measurements for a point and subscribe to receive updates for
		 * measurement changes.
		 *
		 * @param since  Return measurements on or after this time (milliseconds)
		 * @param limit  Max number of measurements returned
		*/
		calls.subscribeToMeasurementHistoryByName = function(pointName, since, limit) {
			return client.subscribeApiRequest({
				request: "subscribeToMeasurementHistoryByName",
				data: {
					pointName: pointName,
					since: since,
					limit: limit
				},
				style: "MULTI",
				resultType: "measurement"
			});
		};
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
				style: "SINGLE",
				resultType: "measurement_statistics"
			});
		};
		////////////////////
		// PointService
		////////////////////
		/**
		 * A Point represents a configured input point for data acquisition. Measurements associated with this
		 * point all use the point name and id. Once obtaining a Point object you should use the MeasurementService
		 * to read/subscribe to the measurements for that point.
		 * <p/>
		 * Every Point is associated with an Entity of type "Point". The point's location in the system
		 * model is determined by this entity. Points are also associated with entities designated as
		 * "logical nodes", which represent the communications interface/source.
		 *
		 * Tag for api-enhancer, do not delete: 
		*/
		/**
		 * get all points in the system
		 *
		 * @return all points
		*/
		calls.getPoints = function() {
			return client.apiRequest({
				request: "getPoints",
				style: "MULTI",
				resultType: "point"
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
				style: "SINGLE",
				resultType: "point"
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
				style: "OPTIONAL",
				resultType: "point"
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
				style: "SINGLE",
				resultType: "point"
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
				style: "MULTI",
				resultType: "point"
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
				style: "MULTI",
				resultType: "point"
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
				style: "MULTI",
				resultType: "point"
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
				style: "MULTI",
				resultType: "point"
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
				style: "MULTI",
				resultType: "point"
			});
		};
		$.extend(client, calls);
	};
})(jQuery);
