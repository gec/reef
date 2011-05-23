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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.util.BuildEnv.ConnInfo
import org.totalgrid.reef.util.ShutdownHook
import org.totalgrid.reef.reactor.{ ReactActor, Lifecycle }

import org.totalgrid.reef.app.{ ApplicationEnroller }

import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.messaging.{ SessionPool, AMQPProtoRegistry, BrokerConnectionInfo, AMQPProtoFactory }

/**
 *  Contains entry point specific code for the measurement processor
 */
object ProcessorEntryPoint extends ShutdownHook {

  def main(args: Array[String]) {

    org.totalgrid.reef.reactor.Reactable.setupThreadPools

    Lifecycle.run(makeContext()) {}
  }

  def makeContext(): List[Lifecycle] = makeContext(BrokerConnectionInfo.loadInfo, MeasurementStoreFinder.getConfig)

  def makeContext(bi: BrokerConnectionInfo, measInfo: ConnInfo): List[Lifecycle] = {
    val amqp = new AMQPProtoFactory with ReactActor {
      val broker = new QpidBrokerConnection(bi)
    }

    val enroller = new ApplicationEnroller(amqp, None, List("Processing"), new FullProcessor(_, measInfo)) with ReactActor

    List(amqp, enroller)
  }

}