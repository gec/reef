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
import org.totalgrid.reef.messaging.BrokerConnectionInfo
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection

import org.totalgrid.reef.messaging.AMQPProtoFactory

import org.totalgrid.reef.reactor.{ ReactActor, LifecycleManager, Lifecycle }
import org.totalgrid.reef.persistence.squeryl.{ SqlProperties, DbInfo, DbConnector }
import org.totalgrid.reef.util.FileConfigReader

import org.totalgrid.reef.measurementstore.MeasurementStoreFinder

object Services {
  def main(argsA: Array[String]): Unit = {

    var args = argsA.toList
    var dbReset = false

    dbReset = !Option(System.getProperty("dbreset")).isEmpty

    if (args.contains("db:reset")) {
      dbReset = true
      args = args.filterNot(_ == "db:reset")
    }

    if (dbReset) {
      //val propFile = Option(System.getProperty("sqlProps")).getOrElse { throw new Exception("Set property sqlProps to reef.sql properties file.") }
      //val dbInfo = SqlProperties.get(new FileConfigReader(propFile))

      val dbInfo = Option(System.getProperty("sqlProps")).map(f => SqlProperties.get(new FileConfigReader(f))).getOrElse(DbInfo.loadInfo)

      resetSystem(dbInfo, dbInfo)
      return
    }

    org.totalgrid.reef.reactor.Reactable.setupThreadPools

    Lifecycle.run(makeContext) {}
  }

  def makeContext(): ServiceContext = makeContext(BrokerConnectionInfo.loadInfo, DbInfo.loadInfo, MeasurementStoreFinder.getConfig, ServiceOptions.loadInfo)

  def makeContext(bi: BrokerConnectionInfo, di: DbInfo, measInfo: ConnInfo, srvOpt: ServiceOptions): ServiceContext = {
    val amqp = new AMQPProtoFactory with ReactActor {
      val broker = new QpidBrokerConnection(bi)
    }

    DbConnector.connect(di)

    new ServiceContext(amqp, measInfo, srvOpt)
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
