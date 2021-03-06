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
 * Proto definition file for Calculations.
 * 
 * <pre>
 * package org.totalgrid.reef.client.service.proto.Calculations;
 * 
 * import "Model.proto";
 * import "Measurements.proto";
 * 
 * option java_package = "org.totalgrid.reef.client.service.proto";
 * option java_outer_classname = "Calculations";
 * 
 * 
 * // only one trigger strategy may be set
 * message TriggerStrategy{
 *     optional uint64       period_ms  = 1;
 *     optional bool         update_any = 3;
 * }
 * 
 * // if 'from' or 'to' is set this is a time based range, other wise it is a samples range
 * // limit can be set for all types
 * message MeasurementRange{
 *     optional  bool        since_last = 4;
 *     optional  uint64      from_ms = 1;
 *     optional  uint64      to_ms   = 2;
 *     optional  uint32      limit   = 3;
 * }
 * 
 * message SingleMeasurement{
 *     enum MeasurementStrategy{
 *         MOST_RECENT = 1;
 *     }
 *     optional MeasurementStrategy  strategy = 1;
 * }
 * 
 * message CalculationInput{
 * 
 *     optional org.totalgrid.reef.client.service.proto.Model.Point             point         = 1;
 *     optional string            variable_name = 2;
 *     optional MeasurementRange  range         = 3;
 *     optional SingleMeasurement single        = 4;
 * }
 * 
 * message InputQuality{
 *     enum Strategy{
 *         ONLY_WHEN_ALL_OK       = 1;
 *         REMOVE_BAD_AND_CALC    = 2;
 *     }
 *     optional Strategy strategy        = 1;
 * }
 * 
 * message OutputQuality{
 *     enum Strategy{
 *         WORST_QUALITY = 1;
 *         ALWAYS_OK     = 2;
 *     }
 *     optional Strategy strategy        = 1;
 * }
 * 
 * message OutputTime{
 *     enum Strategy{
 *         MOST_RECENT     = 1;
 *     }
 *     optional Strategy strategy        = 1;
 * }
 * 
 * message Calculation{
 *     optional org.totalgrid.reef.client.service.proto.Model.ReefUUID          uuid               = 9;
 *     optional org.totalgrid.reef.client.service.proto.Model.Point             output_point       = 1;
 *     optional bool              accumulate         = 2;
 * 
 *     optional TriggerStrategy   triggering         = 3;
 *     repeated CalculationInput  calc_inputs        = 4;
 * 
 *     optional InputQuality      triggering_quality = 5;
 *     optional OutputQuality     quality_output     = 6;
 *     optional OutputTime        time_output        = 7;
 * 
 *     optional string            formula            = 8;
 * }
 * </pre>
 */
package org.totalgrid.reef.client.service.protodoc.calculations;

