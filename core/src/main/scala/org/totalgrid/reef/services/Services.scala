/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services

import org.totalgrid.reef.util.BuildEnv.ConnInfo
import org.totalgrid.reef.broker.BrokerConnectionInfo
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection

import org.totalgrid.reef.messaging.AMQPProtoFactory

import org.totalgrid.reef.executor.{ ReactActorExecutor, LifecycleManager }
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }

import org.totalgrid.reef.measurementstore.MeasurementStoreFinder

import org.totalgrid.reef.sapi.auth.AuthService

object Services {

  def makeContext(auth: AuthService): ServiceContext = makeContext(BrokerConnectionInfo.loadInfo, DbInfo.loadInfo, MeasurementStoreFinder.getConfig, ServiceOptions.loadInfo, auth)

  def makeContext(bi: BrokerConnectionInfo, di: DbInfo, measInfo: ConnInfo, srvOpt: ServiceOptions, auth: AuthService): ServiceContext = {
    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(bi)
    }

    DbConnector.connect(di)

    new ServiceContext(amqp, measInfo, srvOpt, auth)
  }

  def resetSystem(di: DbInfo, measInfo: ConnInfo) {
    DbConnector.connect(di)

    var lifecycleObjects = new LifecycleManager

    val measurementStore = MeasurementStoreFinder.getInstance(measInfo, lifecycleObjects.add)

    lifecycleObjects.start

    ServiceBootstrap.resetDb()
    ServiceBootstrap.seed()
    println("Cleared and updated jvm database")

    if (measurementStore.reset) {
      println("Cleared measurement store")
    } else {
      println("NOTE: measurement store not reset, needs to be done manually")
    }

    lifecycleObjects.stop
  }
}
