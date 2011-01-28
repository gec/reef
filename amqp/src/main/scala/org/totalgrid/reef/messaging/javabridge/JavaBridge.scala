/**
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging.javabridge

import org.totalgrid.reef.messaging._
import org.totalgrid.reef.messaging.sync._
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.ProtoServiceTypes._

import org.totalgrid.reef.reactor.ReactActor

import scala.collection.JavaConversions._

/**
 * A bridge for easily mapping the Scala messaging constructs onto Java constructs
 *    
 */
class JavaBridge(config: BrokerConnectionInfo, timeoutms: Long) extends IJavaBridge {

  /// Scala factory class we're wrapping to simplify access to java clients
  private val factory = new AMQPSyncFactory with ReactActor {
    val broker = new QpidBrokerConnection(config)
  }

  override def addConnectionListener(listener: BrokerConnectionListener) =
    factory.addConnectionListener(listener)

  /**
   *  Starts execution of the messaging connection
   */
  override def start() = factory.start

  /**
   *  Halts execution of the messaging connection
   */
  override def stop() = factory.stop

  def newServiceClient(): IServiceClient = {
    new JavaProtoClientWrapper(new ProtoClient(factory, 5000, ServicesList.getServiceInfo))
  }
}

