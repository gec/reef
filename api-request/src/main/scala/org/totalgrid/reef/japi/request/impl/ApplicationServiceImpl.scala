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
package org.totalgrid.reef.japi.request.impl

import java.util.List
import org.totalgrid.reef.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.japi.request.builders.ApplicationConfigBuilders

import scala.collection.JavaConversions._
import org.totalgrid.reef.japi.client.NodeSettings
import org.totalgrid.reef.japi.request.ApplicationService

trait ApplicationServiceImpl extends ReefServiceBaseClass with ApplicationService {

  override def registerApplication(config: NodeSettings, instanceName: String, capabilities: List[String]) = {
    ops("Failed registering application") {
      _.put(ApplicationConfigBuilders.makeProto(config, instanceName, capabilities.toList)).await().expectOne
    }
  }
  override def sendHeartbeat(ss: StatusSnapshot) = ops("Heartbeat failed") {
    _.put(ss).await().expectOne()
  }
}