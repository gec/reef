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
 * Proto definition file for Utils.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Utils;
 * 
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Utils";
 * 
 * 
 * //  MAJOR TYPES IN THIS PROTO:
 * //
 * //  AttributeList  -- A list of name-value pairs
 * 
 * 
 * // USE CASES:
 * //
 * //  _______________________________________________________
 * //  ...
 * 
 * 
 * // A single attribute with a name and one typed value.
 * //
 * message Attribute {
 * 
 *   enum Type {
 *     STRING  = 1;
 *     SINT64  = 3;   // Signed int up to 64 bits. Unsigned up to 63 bits.
 *     DOUBLE  = 7;   // float and double
 *     BOOL    = 8;
 *     BYTES   = 9;
 *   }
 * 
 *   required string name       = 1;  // Name of the attribute
 *   required Type   vtype      = 2;  // Which value type below
 *   optional string vdescriptor = 3;  // High-level client-specified descriptor: name, ID, phone, date, time, etc.
 * 
 *   // One of the following
 *   optional string  value_string  = 10;
 *   optional sint64  value_sint64  = 11;
 *   optional double  value_double  = 12;
 *   optional bool    value_bool    = 13;
 *   optional bytes   value_bytes   = 14;
 * }
 * 
 * 
 * // A list of attributes.
 * //
 * message AttributeList {
 *   repeated Attribute attribute = 1;  // Repeated name-value pairs.
 * }
 * </pre>
 */
package org.totalgrid.reef.api.japi.client.rpc.protodoc.utils;

