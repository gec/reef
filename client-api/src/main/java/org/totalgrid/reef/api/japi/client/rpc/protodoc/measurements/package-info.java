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
 * Proto definition file for Measurements.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Measurements;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Measurements";
 * 
 * //The descriptions below come from iec61850-7-3{ed1.0}en.pdf, page 15
 * message DetailQual {
 * 	optional bool overflow = 1 [default = false];		// representation overflow, value can't be trusted
 * 	optional bool out_of_range = 2 [default = false];	// different from overlow, occurs when value is outside a predefined range
 * 	optional bool bad_reference = 3 [default = false];	// value may not be correct due to a reference being out of calibration
 * 	optional bool oscillatory = 4 [default = false];	// value is rapidly changing and some values may have be suppressed
 * 	optional bool failure = 5 [default = false];		// a supervision function has detected an internal or external failure
 * 	optional bool old_data = 6 [default = false];		// an update was not made during a specific time internal
 * 	optional bool inconsistent = 7 [default = false];	// an evaluation function has detected an inconsistency
 * 	optional bool inaccurate = 8 [default = false];		// value does not meet the stated accuracy of the source
 * }
 * 
 * // mirror the iec61850 quality (CIM uses these too)
 * message Quality {
 * 	enum Validity {
 * 		GOOD = 0;		// No abormal condition of the acquisition function or the information source is detected	
 * 		INVALID = 1;		// Abornormal condition ""
 * 		QUESTIONABLE = 2;	// supervision function detects abnormal behavior, however value could still be valid. Up to client how to interpret.
 * 	}
 * 
 * 	enum Source {
 * 		PROCESS = 0;	 // value is provided by an input function from the process I/O or calculated by application
 * 		SUBSTITUTED = 1; // value is provided by input on an operator or by an automatic source
 * 	}
 * 
 * 	optional Validity validity = 1 [default = GOOD];
 * 	optional Source source = 2 [default = PROCESS];
 * 	optional DetailQual detail_qual = 3;
 * 	optional bool test = 4 [default = false];		// classifies a value as a test value, not to be used for operational purposes
 * 	optional bool operator_blocked = 5 [default = false];	// further update of the value has been blocked by an operator. if set, DetailQual::oldData should be set
 * }
 * 
 * // fundamental measurement type used in the system
 * message Measurement {   
 * 	
 * 	optional string name = 1;	// name of the measurement, specific to the system's modeling conventions
 * 
 * 	enum Type {
 * 		INT = 0;     // int_val will be set
 * 		DOUBLE = 1;  // double_val will be set
 * 		BOOL = 2;    // bool_val will be set
 * 		STRING = 3;  // string_val will be set
 * 		NONE = 4;    // a measurement can have no value if never set or stripped by the meas_proc
 * 	}
 * 		
 * 	// points to one of the following fields of which 0 or more should be set. If more
 * 	// than one is set this type field specifies the "canonical" type that should be persisted
 * 	required Type type = 2; 
 * 	
 * 	optional sint64 int_val = 3;	// maps to protocol integer types (i.e. DNP Analog, counter, etc)
 * 	optional double double_val = 4; // maps to protocol double types
 * 	optional bool bool_val = 5;	// maps to protocol bool types, i.e. DNP status
 * 	optional string string_val = 6;	// maps to arbitrary string types
 * 
 * 	required Quality quality = 7; 	// use 61850 quality type definitions
 * 	optional string unit = 8 [default = "raw"];
 * 	
 * 	optional uint64 time = 9 [default = 0]; // Unix time, but with milliseconds
 * 	optional bool is_device_time = 10 [default = false]; // true if time field is a device timestamp, false or unset if wall time
 * }
 * 
 * // measurements are sent from the FEP in batches determined by protocol structure
 * // The wall time is for the batch and can be used to apply time to the 
 * // measurements if the device time is missing
 * message MeasurementBatch {
 * 	required uint64 wall_time = 1; // FEP wall time
 * 	repeated Measurement meas = 2; // ordering is protocol retrieval order
 * }
 * 
 * // A type designed to archive a set of measurements
 * message MeasArchiveUnit {
 * 	optional sint64 int_val = 1;
 * 	optional sint64 double_val = 2; // double encoded as long bits 	
 * 	optional bool bool_val = 3;
 * 	optional string string_val = 4;
 * 	optional Quality quality = 5;
 * 	required uint64 time = 6;
 * }
 * 
 * // A type designed to archive a set of measurements
 * message MeasArchive {		
 * 	repeated MeasArchiveUnit meas = 1;
 * }
 * 
 * // Service type that returns the current state of N points by Name
 * message MeasurementSnapshot{
 *     repeated string      point_names  = 2;
 *     repeated Measurement measurements = 3;
 * }
 * 
 * // Service type that returns a time slice for a single measurement
 * message MeasurementHistory{
 *     required string      point_name   = 1;
 *     optional uint64      start_time   = 2 [default = 0];
 *     optional uint64      end_time     = 3 [default = 0];
 *     optional uint32      limit        = 4 [default = 0];
 *     // if the limit stops us from getting all measurements we will keep
 *     // the newest measurements instead of the oldest ones.
 *     optional bool        keep_newest  = 5 [default = true]; 
 *     enum Sampling{
 *       NONE = 0;
 *       EXTREMES = 1;
 *     }
 *     optional Sampling    sampling     = 6 [default = NONE];
 *     
 *     // measurements are always returned in ascending time order (oldest first)
 *     repeated Measurement measurements = 7;
 * }
 * </pre>
 */
package org.totalgrid.reef.api.japi.client.rpc.protodoc.measurements;

