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
 * Proto definition file for MeasurementMapping.
 * 
 * <pre>
 * package org.totalgrid.reef.api.proto.Mapping;
 * 
 * option java_package = "org.totalgrid.reef.api.proto";
 * option java_outer_classname = "Mapping";
 * 
 * enum DataType {
 *     ANALOG          = 1;
 *     BINARY          = 2;
 *     COUNTER         = 3;
 *     CONTROL_STATUS  = 4;
 *     SETPOINT_STATUS = 5;
 * }
 * 
 * enum CommandType {
 *     PULSE       = 1;
 *     LATCH_ON    = 2;
 *     LATCH_OFF   = 3;
 *     PULSE_CLOSE = 4;
 *     PULSE_TRIP  = 5;
 *     SETPOINT    = 6;
 * }
 * 
 * message MeasMap {
 *     required DataType    type       = 1;
 *     required uint32      index      = 2;
 *     required string      point_name = 3;
 *     optional string      unit       = 4; // this is the "raw" unit of the measurement.
 * }
 * 
 * message CommandMap {
 *     required CommandType    type         = 1;
 *     required uint32         index        = 2;
 *     required string         command_name = 3;
 *     optional uint32         on_time      = 4 [default = 100];
 *     optional uint32         off_time     = 5 [default = 100];
 *     optional uint32         count        = 6 [default = 1];
 * }
 * 
 * message IndexMapping {
 *     //optional string           uid          = 1;
 *     optional string           device_uid   = 2;
 *     repeated MeasMap          measmap      = 3;
 *     repeated CommandMap       commandmap   = 4;
 * }
 * </pre>
 */
package org.totalgrid.reef.api.japi.client.rpc.protodoc.measurementmapping;

