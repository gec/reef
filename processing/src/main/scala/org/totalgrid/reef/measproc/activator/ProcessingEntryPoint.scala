/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measproc.activator

import org.totalgrid.reef.api.japi.client.{ NodeSettings, UserSettings }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionInfo
import org.totalgrid.reef.persistence.squeryl.SqlProperties
import org.totalgrid.reef.app.ConnectionCloseManagerEx
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.util.ShutdownHook
import org.totalgrid.reef.api.sapi.impl.FileConfigReader

object ProcessingEntryPoint extends ShutdownHook {
  def main(args: Array[String]) = {
    val brokerOptions = QpidBrokerConnectionInfo.loadInfo(new FileConfigReader("org.totalgrid.reef.amqp.cfg"))
    val userSettings = new UserSettings(new FileConfigReader("org.totalgrid.reef.user.cfg").props)
    val nodeSettings = new NodeSettings(new FileConfigReader("org.totalgrid.reef.node.cfg").props)

    val dbInfo = SqlProperties.get(new FileConfigReader("org.totalgrid.reef.sql.cfg"))

    //    val measExec = new ReactActorExecutor {}
    //    val measStore = MeasurementStoreFinder.getInstance(dbInfo, measExec, context)
    //    measExec.start

    val measStore = new InMemoryMeasurementStore

    val manager = new ConnectionCloseManagerEx(brokerOptions)

    manager.addConsumer(ProcessingActivator.createMeasProcessor(userSettings, nodeSettings, measStore))

    //    measExec.start
    manager.start

    waitForShutdown {
      manager.stop
      //      measExec.stop
    }
  }
}