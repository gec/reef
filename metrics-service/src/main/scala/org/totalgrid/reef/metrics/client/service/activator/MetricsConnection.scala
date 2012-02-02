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
package org.totalgrid.reef.metrics.client.service.activator

import org.totalgrid.reef.app.ConnectionConsumer
import org.totalgrid.reef.broker.BrokerConnection
import net.agileautomata.executor4s.{ Cancelable, Executor }
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings }
import org.totalgrid.reef.metrics.client.service.MetricsService
import org.totalgrid.reef.metrics.client.MetricsServiceList
import org.totalgrid.reef.client.AnyNodeDestination

class MetricsConnection(user: UserSettings, node: NodeSettings) extends ConnectionConsumer {

  def newConnection(brokerConnection: BrokerConnection, exe: Executor): Cancelable = {
    val connection = new DefaultConnection(brokerConnection, exe, 5000)
    connection.addServicesList(new ReefServices)
    connection.addServicesList(new MetricsServiceList)

    val client = connection.login(user).await.getRpcInterface(classOf[AllScadaService])

    val appConfig = client.registerApplication(node, node.getDefaultNodeName + "-Metrics", List("Services")).await

    val heartbeater = new ProcessHeartbeatActor(client, appConfig.getHeartbeatCfg, exe)

    connection.bindService(new MetricsService, exe, new AnyNodeDestination, true)

    heartbeater.start()

    new Cancelable {
      def cancel() = {
        heartbeater.stop()
      }
    }
  }
}