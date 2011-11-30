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
 * Proto definition file for SimulatorMapping.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Simulator;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "SimMapping";
 * 
 * import "Measurements.proto";
 * import "Commands.proto";
 * 
 * message MeasSim {
 *     
 *     required string      name         = 1; // fully qualified name of the point
 *     required string      unit         = 2; 
 *     required org.totalgrid.reef.proto.Measurements.Measurement.Type type = 3;
 * 
 *     optional double      initial      = 4; // if status use 0 or 1
 *     optional double      min          = 5; 
 *     optional double      max          = 6; 
 *     optional double      max_delta     = 7; // largest jump, needs to be atleast 1 for INT measurements
 *     optional double      change_chance = 8; // odds of the point changing, 0 would never change, 1.0 changes everytime
 * }
 * 
 * message CommandSim {
 *     required string      name         = 1; // fully qualified name of the command
 *     // command code to return to commands
 *     required org.totalgrid.reef.proto.Commands.CommandStatus  response_status  = 2;
 * }
 * 
 * message SimulatorMapping {
 *     required uint32      delay         = 1;
 *     
 *     repeated MeasSim     measurements  = 3;
 *     repeated CommandSim  commands      = 4;
 * }
 * </pre>
 */
package org.totalgrid.reef.client.service.protodoc.simulatormapping;

