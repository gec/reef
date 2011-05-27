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
 * Proto definition file for AuthManagement.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Auth;
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Auth";
 * 
 * import "Model.proto";
 * 
 * message Agent{
 *     optional org.totalgrid.reef.proto.Model.ReefUUID       uuid                = 1;
 *     optional string name               = 2;
 *     optional string password           = 3;
 *     repeated PermissionSet permission_sets = 4;
 * }
 * 
 * message Permission{
 *     optional string  uid      = 1;
 *     optional bool    allow    = 2;
 *     optional string  resource = 3;
 *     optional string  verb     = 4;
 * }
 * 
 * message PermissionSet{
 *     optional org.totalgrid.reef.proto.Model.ReefUUID       uuid                     = 1;
 *     optional string     name                    = 2;
 *     optional uint64     default_expiration_time = 3;
 *     repeated Permission permissions             = 4;
 * }
 * 
 * message AuthToken{
 *     optional string        uid             = 1;
 *     optional Agent         agent           = 2;
 *     optional string        login_location  = 3;
 *     repeated PermissionSet permission_sets = 4;
 *     optional string        token           = 5;
 *     optional uint64        expiration_time = 6;
 * }
 * </pre>
 */
package org.totalgrid.reef.japi.request.protodoc.authmanagement;

