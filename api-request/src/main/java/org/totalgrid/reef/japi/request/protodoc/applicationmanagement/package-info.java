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
 * Proto definition file for ApplicationManagement.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Application;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Application";
 * 
 * import "Model.proto";
 * 
 * message HeartbeatConfig{
 *     optional string process_id    = 1;
 *     optional string dest          = 2;
 *     optional uint32 period_ms     = 3;
 *     optional string routing_key   = 4;
 *     optional string instance_name = 5;
 * }
 * 
 * message StreamServicesConfig{
 *     required string logs_dest    = 1;
 *     required string events_dest  = 2;
 *     required string nonop_dest   = 3;
 * }
 * 
 * message ApplicationConfig{
 *     optional org.totalgrid.reef.proto.Model.ReefUUID       uuid           = 1;
 *     optional string user_name     = 2; // bus context user name
 *     optional string instance_name = 3; // name of this processing, should be unique per system
 *     optional string process_id    = 9; // a process identifier that shows when the containg process goes down
 *                                        // NOTE: does not need to be process id, a random number is suffecient
 *     optional string network       = 4; // the network name should indicate what ip addresses are reachable
 *     optional string location      = 5; // usually machine name, so we know what serial ports are attached
 *     repeated string capabilites   = 6; // List of capabilites offered by the node, FEP, Services, MeasProc, etc.
 *     optional HeartbeatConfig      heartbeat_cfg = 7;
 *     optional StreamServicesConfig stream_cfg    = 8;
 * }
 * </pre>
 */
package org.totalgrid.reef.japi.request.protodoc.applicationmanagement;

