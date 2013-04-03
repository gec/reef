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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.client.sapi.rpc.impl.builders.ApplicationConfigBuilders
import org.totalgrid.reef.client.operations.scl.UsesServiceOperations
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._

import org.totalgrid.reef.client.sapi.rpc.ApplicationService
import org.totalgrid.reef.client.settings.{ Version, NodeSettings }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.service.proto.Model.ReefUUID

trait ApplicationServiceImpl extends UsesServiceOperations with ApplicationService {

  override def registerApplication(config: NodeSettings, instanceName: String, capabilities: List[String]) = {
    ops.operation("Failed registering application") {
      _.put(ApplicationConfigBuilders.makeProto(Version.getClientVersion, config, instanceName, capabilities.toList)).map(_.one)
    }
  }
  override def registerApplication(version: String, config: NodeSettings, instanceName: String, capabilities: List[String]) = {
    ops.operation("Failed registering application") {
      _.put(ApplicationConfigBuilders.makeProto(version, config, instanceName, capabilities.toList)).map(_.one)
    }
  }
  override def unregisterApplication(appConfig: ApplicationConfig) = {
    ops.operation("Failed removing application") {
      _.delete(appConfig).map(_.one)
    }
  }
  override def sendHeartbeat(statusSnapshot: StatusSnapshot) =
    ops.operation("Heartbeat failed")(_.put(statusSnapshot).map(_.one))

  override def sendHeartbeat(appConfig: ApplicationConfig) =
    ops.operation("Heartbeat failed")(_.put(makeStatusProto(appConfig, true)).map(_.one))

  def sendApplicationOffline(appConfig: ApplicationConfig) =
    ops.operation("Setting app offline failed")(_.put(makeStatusProto(appConfig, false)).map(_.one))

  private def makeStatusProto(appConfig: ApplicationConfig, online: Boolean): StatusSnapshot = {
    StatusSnapshot.newBuilder
      .setProcessId(appConfig.getProcessId)
      .setInstanceName(appConfig.getInstanceName)
      .setTime(System.currentTimeMillis)
      .setOnline(online).build
  }

  override def getApplications() = ops.operation("Heartbeat failed") {
    _.get(ApplicationConfig.newBuilder.setInstanceName("*").build).map(_.many)
  }

  def findApplicationByName(name: String) = ops.operation("Couldn't find application with name: " + name) {
    _.get(ApplicationConfig.newBuilder.setInstanceName(name).build).map(_.oneOrNone)
  }

  def getApplicationByName(name: String) = ops.operation("Couldn't get application with name: " + name) {
    _.get(ApplicationConfig.newBuilder.setInstanceName(name).build).map(_.one)
  }

  def getApplicationByUuid(uuid: ReefUUID) = ops.operation("Couldn't get application with uuid: " + uuid.getValue) {
    _.get(ApplicationConfig.newBuilder.setUuid(uuid).build).map(_.one)
  }
}