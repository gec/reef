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
 * Proto definition file for Processing.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Processing;
 * 
 * import "Model.proto";
 * import "Measurements.proto";
 * import "ApplicationManagement.proto";
 * option java_package = "org.totalgrid.reef.proto";
 * 
 * // To place a point in the OperatorBlocked state (aka. Not In Service),
 * // send a MeasOverride without a Measurement.
 * //
 * message MeasOverride {
 *   required org.totalgrid.reef.proto.Model.Point point = 1;
 *   optional org.totalgrid.reef.proto.Measurements.Measurement meas = 2;
 * }
 * 
 * enum ActivationType {
 *     HIGH       = 1;
 *     LOW        = 2;
 *     RISING     = 3;
 *     FALLING    = 4;
 *     TRANSITION = 5;
 * }
 * 
 * message Action {
 *   required string action_name = 1;
 *   optional ActivationType type = 2;
 *   optional bool disabled = 3;
 *   
 *   optional LinearTransform                          linear_transform = 10;
 *   optional org.totalgrid.reef.proto.Measurements.Quality      quality_annotation = 11;
 *   optional bool                                     strip_value = 12;
 *   optional bool                                     set_bool = 13;
 *   optional string                                   set_unit = 14;  // "to" unit
 *   optional EventGeneration                          event = 15;
 *   optional BoolEnumTransform                        bool_transform   =  16;
 *   optional IntEnumTransform                         int_transform    =  17;
 * }
 * 
 * message LinearTransform {
 *   optional double scale = 2;
 *   optional double offset = 3;
 * }
 * 
 * message EventGeneration {
 *   optional string event_type   = 1;
 *   optional uint32 severity     = 2;
 * }
 * 
 * // When the system detects a "trigger", it will perform some "actions".
 * // Triggers are:  range, scale, unit, etc.
 * //
 * // Scaling a raw measurement:
 * //   Trigger.unit = <from unit>
 * //   Action.linear_transform.scale = slope
 * //   Action.linear_transform.offset = offset
 * //   Action.set_unit = <to unit>
 * // Setting the engineering unit for a raw measurement that's already the correct scale
 * //   Trigger.unit = <raw unit>
 * //   Action.set_unit = <engineering unit>
 * //
 * message Trigger {
 *   optional string              trigger_name = 2;         // unique name for each trigger in a TriggerSet.
 *   optional ActivationType      stop_processing_when = 3; // don't process more triggers on this point
 *   optional uint32              priority = 4;             // process triggers in order of priority (lowest number to highest)
 *   
 *   repeated Action              actions = 9;
 * 
 *   // Types of triggers:
 *   //
 *   optional AnalogLimit                                    analog_limit = 10;
 *   optional org.totalgrid.reef.proto.Measurements.Quality            quality = 12;
 *   optional string                                         unit = 13;       // "from" unit
 *   optional org.totalgrid.reef.proto.Measurements.Measurement.Type   value_type = 14;
 *   optional bool                                           bool_value = 15;
 *   optional string                                         string_value = 16;
 *   optional sint64                                         int_value    = 17;
 * }
 * 
 * // A set of triggers for a point.
 * // There is one TriggerSet per point, so 'putting' a TriggerSet overwrites any
 * // previous TriggerSet for said point.
 * //
 * message TriggerSet {
 *   optional org.totalgrid.reef.proto.Model.ReefUUID       uuid = 3;
 *   optional org.totalgrid.reef.proto.Model.Point point = 1;
 *   repeated Trigger triggers = 2;
 * }
 * 
 * message AnalogLimit {
 *   optional double upper_limit = 1;
 *   optional double lower_limit = 2;
 *   optional double deadband    = 3;
 * }
 * 
 * message BoolEnumTransform {
 *   required string true_string  = 1;
 *   required string false_string = 2;
 * }
 * 
 * message IntEnumTransform {
 *   repeated IntToString mappings = 1;
 * }
 * 
 * message IntToString {
 *   required sint64 value       = 1;
 *   required string string      = 2;
 * }
 * 
 * 
 * message MeasurementProcessingRouting{
 *   optional string service_routing_key   = 1;
 *   optional string processed_meas_dest = 2;
 *   optional string raw_event_dest      = 3;
 * }
 * 
 * message MeasurementProcessingConnection{
 *     optional string                       uid                   = 1;
 *     optional org.totalgrid.reef.proto.Application.ApplicationConfig meas_proc = 2;
 *     optional org.totalgrid.reef.proto.Model.Entity logicalNode         = 3;
 *     optional MeasurementProcessingRouting routing               = 5;
 *     optional uint64                       assignedTime          = 6;
 *     optional uint64                       readyTime             = 7;
 * }
 * </pre>
 */
package org.totalgrid.reef.api.japi.client.rpc.protodoc.processing;

