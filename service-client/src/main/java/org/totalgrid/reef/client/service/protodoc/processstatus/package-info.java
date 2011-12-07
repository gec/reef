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
 * Proto definition file for ProcessStatus.
 * 
 * <pre>
 * package org.totalgrid.reef.client.service.proto.ProcessStatus;
 * 
 * option java_package = "org.totalgrid.reef.client.service.proto";
 * import "Model.proto";
 * 
 * message StatusSnapshot {
 *   optional string process_id     = 1;
 *   optional string instance_name = 2;
 *   optional bool online = 3;
 *   optional uint64 time = 4 [default = 0];
 * }
 * </pre>
 */
package org.totalgrid.reef.client.service.protodoc.processstatus;

