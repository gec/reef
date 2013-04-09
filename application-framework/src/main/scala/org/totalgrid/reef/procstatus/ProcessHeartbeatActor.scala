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
package org.totalgrid.reef.procstatus

import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot

import org.totalgrid.reef.client.service.proto.Application.HeartbeatConfig
import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.client.sapi.rpc.ApplicationService
import org.totalgrid.reef.client.exception.ReefServiceException

import net.agileautomata.executor4s._
import org.totalgrid.reef.util.Lifecycle

class ProcessHeartbeatActor(services: ApplicationService, configuration: HeartbeatConfig, exe: Executor)
    extends Lifecycle with Logging {

  private def makeProto(online: Boolean): StatusSnapshot = {
    StatusSnapshot.newBuilder
      .setProcessId(configuration.getProcessId)
      .setInstanceName(configuration.getInstanceName)
      .setTime(System.currentTimeMillis)
      .setOnline(online).build
  }

  private var repeater: Option[Timer] = None

  override def afterStart() = {
    repeater = Some(exe.scheduleWithFixedOffset(configuration.getPeriodMs.milliseconds)(heartbeat()))
  }

  override def beforeStop() = {
    repeater.foreach(_.cancel)
    publish(makeProto(false))
  }

  private def heartbeat() = publish(makeProto(true))

  private def publish(ss: StatusSnapshot) {
    try {
      services.sendHeartbeat(ss).await
    } catch {
      case rse: ReefServiceException =>
        logger.warn("Problem sending heartbeat: " + rse.getMessage)
    }
  }

}