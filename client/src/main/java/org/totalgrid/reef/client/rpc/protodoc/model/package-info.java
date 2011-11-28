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
 * Proto definition file for Model.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Model;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Model";
 * 
 * import "Utils.proto";
 * 
 * 
 * /*
 *  UUIDs are used for "long lasting" elements in the system that are we expect
 *  to not grow in value
 * -/
 * message ReefUUID {
 *   required string uuid = 1;
 * }
 * 
 * /*
 *   Items in the system that are not expected to
 * -/
 * message ReefID {
 *   required string value = 1;
 * }
 * 
 * /*
 *   Entity
 * 
 *   Represents a generic component of a system model. Entities are modeled by a name
 *   and some number of types which describe the entity's role in the system. Entities
 *   also contain a list of Relationship objects which describe connections/associations
 *   with other entities in the system.
 * 
 *   Together, entities connected by a certain relationship type form an acyclic directed
 *   graph. The model must not include cycles (given a start entity, it must not be possible
 *   to get back to the same entity using the same relationship type and direction).
 * -/
 * message Entity {
 *   optional ReefUUID uuid = 1;
 *   repeated string types = 2;
 *   optional string name = 3;
 *   repeated Relationship relations = 10;
 * }
 * 
 * 
 * /*
 *   Relationship
 * 
 *   Entity relationships have a type ("relationship"), a direction ("descendant_of"), and
 *   a list of Entity objects the relationship connects to. Because relationships are transitive,
 *   they also have a distance, or how many edges away the other entities are.
 * -/
 * message Relationship {
 *   optional string relationship = 1;
 *   optional bool descendant_of = 2;
 *   repeated Entity entities = 10;
 *   optional uint32 distance = 15;
 * }
 * 
 * 
 * /*
 *   EntityEdge
 * 
 *   Represents a single directed edge between entities. Puts to the service automatically
 *   enforce the transitive property of entity relationships.
 * -/
 * message EntityEdge {
 *   optional ReefUUID uuid = 1;
 *   optional Entity parent = 2;
 *   optional Entity child = 3;
 *   optional string relationship = 4;
 * }
 * 
 * 
 * message EntityAttributes {
 *   optional Entity entity = 1;
 *   repeated org.totalgrid.reef.proto.Utils.Attribute attributes = 2;
 * }
 * 
 * enum PointType {
 *   ANALOG         = 1;
 *   COUNTER        = 2;
 *   STATUS         = 3;
 * }
 * enum CommandType {
 *   CONTROL         = 1;
 *   SETPOINT_INT    = 2;
 *   SETPOINT_DOUBLE = 3;
 * }
 * 
 * /*
 *   Point
 * 
 *   Represents a configured input point for data acquisition. Measurements associated with
 *   this point all use the point name.
 * 
 *   Every Point is associated with an Entity of type "Point". The point's location in the system
 *   model is determined by this entity. Points are also associated with entities designated as
 *   "logical nodes", which represent the communications interface/source.
 * 
 * -/
 * message Point {
 *   optional ReefUUID uuid = 1;
 *   optional string  name = 2;
 *   optional Entity logicalNode = 5; // live state
 *   optional Entity entity   = 7;    // The point entity
 *   optional bool    abnormal = 6;   // live state
 *   optional PointType type = 8;
 *   optional string    unit = 9;
 * }
 * 
 * 
 * /*
 *   Command
 * 
 *   Represents a configured output point. CommandAccess and UserCommandRequest services use
 *   this command name.
 * 
 *   Every Command is associated with an Entity of type "Command". The command's location in the
 *   system model is determined by this entity. Commands are also associated with entities designated
 *   as "logical nodes", which represent the communications interface/source.
 * 
 * -/
 * message Command {
 *   optional ReefUUID uuid = 1;
 *   optional string  name = 2;
 *   optional string display_name = 3;
 *   optional Entity logicalNode = 5;
 *   optional Entity entity   = 6;
 *   optional CommandType type = 7;
 * }
 * 
 * 
 * /*
 *   ConfigFile
 * 
 *   Contains configuration information for an application (i.e. a protocol implementation). The
 *   configuration and the application must agree on how to interpret the data, which is treated
 *   as an opaque byte array by the services.
 * 
 *   ConfigFile objects also contain a reference to the entities they are associated with.
 * -/
 * message ConfigFile {
 *     optional ReefUUID uuid    = 1;
 *     optional string name      = 2;
 *     optional string mime_type = 3;
 *     optional bytes  file      = 4;
 *     repeated Entity entities  = 5;
 * }
 * </pre>
 */
package org.totalgrid.reef.client.rpc.protodoc.model;

