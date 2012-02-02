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
package org.totalgrid.reef.standalone

import org.totalgrid.reef.measurementstore.squeryl.SqlMeasurementStore
import org.totalgrid.reef.simulator.random.{ DefaultSimulator, DefaultSimulatorFactory }
import org.totalgrid.reef.protocol.simulator.SimulatedProtocol
import org.totalgrid.reef.protocol.dnp3.master.Dnp3MasterProtocol
import org.totalgrid.reef.protocol.api.{ AddRemoveValidation, ChannelAlwaysOnline, EndpointAlwaysOnline, Protocol }
import org.totalgrid.reef.protocol.dnp3.slave.Dnp3SlaveProtocol
import org.totalgrid.reef.client.settings.util.PropertyLoading
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.client.settings.AmqpSettings
import org.totalgrid.reef.broker.memory.MemoryBrokerConnectionFactory
import org.totalgrid.reef.measurementstore._
import java.util.Properties
import net.agileautomata.executor4s.{ Executor, Cancelable }
import net.agileautomata.executor4s.testing.MockExecutorService
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }

object NullCancelable extends Cancelable {
  def cancel() {}
}

/**
 * implementing some implementation lookups to replace OSGi service lookups
 */
object ImplLookup extends Logging {
  def loadProtocols(properties: Properties, exe: Executor): List[Protocol] = {
    def getProtocolImpl(typ: String): Protocol = typ match {
      case "benchmark" =>
        val simFactory = new DefaultSimulatorFactory({ t: DefaultSimulator => NullCancelable })
        val simProtocol = new SimulatedProtocol(exe) with EndpointAlwaysOnline with ChannelAlwaysOnline
        simProtocol.addPluginFactory(simFactory)
        simProtocol
      case "dnp3" =>
        System.loadLibrary("dnp3java")
        System.setProperty("reef.api.protocol.dnp3.nostaticload", "")
        new Dnp3MasterProtocol with AddRemoveValidation
      case "dnp3-slave" =>
        System.loadLibrary("dnp3java")
        System.setProperty("reef.api.protocol.dnp3.nostaticload", "")
        new Dnp3SlaveProtocol with AddRemoveValidation
    }
    val protocolNames = PropertyLoading.getString("org.totalgrid.reef.protocols", properties, "benchmark,dnp3,dnp3-slave")
    protocolNames.split(",").toList.map { getProtocolImpl(_) }
  }

  def loadBrokerConnection(properties: Properties, exe: Executor) = {
    val brokerType = PropertyLoading.getString("org.totalgrid.reef.amqp.type", properties, "qpid")
    logger.info("Broker: " + brokerType)
    brokerType match {
      case "qpid" => new QpidBrokerConnectionFactory(new AmqpSettings(properties))
      case "memory" => new MemoryBrokerConnectionFactory(exe)
    }
  }

  def loadMeasurementStore(properties: Properties, exe: Executor) = {

    val historianType = PropertyLoading.getString("org.totalgrid.reef.mstore.historianImpl", properties)
    val currentValueType = PropertyLoading.getString("org.totalgrid.reef.mstore.currentValueImpl", properties)

    def getMeasImpl(typ: String) = typ match {
      case "squeryl" => new SqlMeasurementStore({ () => DbConnector.connect(new DbInfo(properties)) })
      case "memory" => new InMemoryMeasurementStore()
    }
    logger.info("MeasStore historian: " + historianType + " realtime: " + currentValueType)
    if (historianType == currentValueType) {
      getMeasImpl(historianType)
    } else {
      new MixedMeasurementStore(new MockExecutorService(exe), getMeasImpl(historianType), getMeasImpl(currentValueType))
    }
  }
}