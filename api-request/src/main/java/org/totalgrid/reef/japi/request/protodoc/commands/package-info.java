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
/**
 * Proto definition file for Commands.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Commands;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Commands";
 * 
 * import "Model.proto";
 * 
 * /*
 *   UserCommandRequest
 * 
 *   Clients use put to issue a command. The CommandRequest object describes the command
 *   to be executed, and timeout can be specified by the client code.
 * 
 *   Status and user are not specified in put. User is identified from the request header.
 * 
 * -/
 * message UserCommandRequest {
 * 
 * 	optional string         uid             = 1;
 * 	optional CommandRequest command_request = 2;
 * 	optional CommandStatus  status          = 3;
 * 	optional string         user            = 4;
 * 	optional int32          timeout_ms      = 5 [default = 5000];
 * }
 * 
 * /*
 *   CommandAccess
 * 
 *   Represents the "access table" for the system. Access entries have one or two
 *   modes, "allowed" and "blocked". Commands cannot be issued unless they have an
 *   "allowed" entry. This "selects" the command for operation by a single user, for
 *   as long as access is held. "Block" allows selects to be disallowed for commands;
 *   meaning no users can access/issue the commands.
 * 
 *   Multiple commands can be referenced (by name) in the same access entry. User is
 *   determined by the request header.
 * 
 *   If not provided, expire_time will be a server-specified default.
 * 
 * -/
 * message CommandAccess {
 *   enum AccessMode {
 *     ALLOWED = 1;
 *     BLOCKED = 2;
 *   }
 *   optional string       uid             = 1;
 *   repeated string       commands        = 2;
 *   optional AccessMode   access          = 3;
 *   optional uint64       expire_time     = 4;
 *   optional string       user            = 5;
 * }
 * 
 * 
 * /*
 *   CommandRequest
 * 
 *   For services, CommandRequest is an attribute of UserCommandRequest. The object is also
 *   used for the interaction between the services and the FEP.
 * 
 *   Commands take two forms: without a value attached and with a value attached. Commands with
 *   value attached are the equivalent of "setpoint" or "analog output" in SCADA systems. "Controls"
 *   or "binary outputs" are modeled in the system as two value-less commands, i.e. one for "high" and
 *   one for "low".
 * 
 *   Correlation ID is not used by client requests.
 * 
 * -/
 * 
 * message CommandRequest {
 * 
 * 	enum ValType {
 * 		INT = 1;
 * 		DOUBLE = 2;
 * 		NONE = 3;
 * 	}
 * 	
 * 	// unique name of the control in the system. Whomever executes the control knows protocol specific details (if necessary)
 * 	optional string  name = 1;
 * 
 * 	// correlation id for the request, used to match requests to response
 * 	// field used for service/FEP interaction, not client/server interaction
 * 	optional string  correlation_id = 2;
 * 	
 * 	optional ValType type = 3;
 * 	optional int32   int_val = 4;
 * 	optional double  double_val = 5;
 * }
 * 
 * /*
 *   CommandStatus
 * 
 *   Enumeration for the current status of a command request. Uses DNP3 control response codes.
 * 
 *   Normal operation involves the lifecycle EXECUTING -> SUCCESS. Failure will result in one of
 *   the error conditions (TIMEOUT, NOT_AUTHORIZED, etc.)
 * 
 * -/
 * enum CommandStatus {
 * 	SUCCESS        = 1;
 * 	TIMEOUT        = 2;
 * 	NO_SELECT      = 3;
 * 	FORMAT_ERROR   = 4;
 * 	NOT_SUPPORTED  = 5;
 * 	ALREADY_ACTIVE = 6;
 * 	HARDWARE_ERROR = 7;
 * 	LOCAL          = 8;
 * 	TOO_MANY_OPS   = 9;
 * 	NOT_AUTHORIZED = 10;
 * 	UNDEFINED      = 11;
 * 	EXECUTING      = 12;
 * }
 * 
 * /*
 *   CommandResponse
 * 
 *   Used for communication between the services and FEPs. Indicates the result of a command request.
 * 
 * -/
 * message CommandResponse {
 * 	required string        correlation_id = 1; // needs to match id of request
 * 	required CommandStatus status         = 2; // status code
 * }
 * </pre>
 */
package org.totalgrid.reef.japi.request.protodoc.commands;

