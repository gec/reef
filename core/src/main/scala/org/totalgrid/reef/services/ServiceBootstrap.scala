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

import org.totalgrid.reef.app.{ CoreApplicationComponents, ApplicationEnroller }
import org.totalgrid.reef.protoapi.RequestEnv
import org.totalgrid.reef.protoapi.ServiceHandlerHeaders._

import org.totalgrid.reef.proto.FEP.FrontEndProcessor
import org.totalgrid.reef.messaging.{ ServicesList, AMQPProtoFactory }
import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry

object ServiceBootstrap {
  /** 
   * since _we_are_ a service provider we can create whatever services we would normally
   * use to enroll ourselves as an application to get the CoreApplicationComponents without
   * repeating that setup logic somewhere else
   */
  def bootstrapComponents(amqp: AMQPProtoFactory): CoreApplicationComponents = {
    val pubs = new ServiceEventPublisherRegistry(amqp, ServicesList.getServiceInfo)
    val modelFac = new core.ModelFactories(pubs, new core.SilentSummaryPoints)
    val applicationConfigService = new core.ApplicationConfigService(modelFac.appConfig)
    val authService = new core.AuthTokenService(modelFac.authTokens)

    val login = ApplicationEnroller.buildLogin()
    val authToken = authService.put(login).result.head

    val config = ApplicationEnroller.buildConfig(List("Services"))
    val appConfig = applicationConfigService.put(config).result.head

    // the measurement batch service acts as a type of manual FEP
    val msg = FrontEndProcessor.newBuilder
    msg.setAppConfig(appConfig)
    msg.addProtocols("null")
    val fepService = new core.FrontEndProcessorService(modelFac.fep)
    fepService.put(msg.build)

    val env = new RequestEnv
    env.addAuthToken(authToken.getToken)
    new CoreApplicationComponents(amqp, appConfig, env)
  }

  /**
   * sets up the default users and low level configurations for the system 
   */
  def seed() {
    core.EventConfigService.seed()
    core.AuthTokenService.seed()
  }

  /**
   * drops and re-creates all of the tables in the database.
   */
  def resetDb() {
    import org.squeryl.PrimitiveTypeMode._
    import org.totalgrid.reef.models._
    transaction {
      ApplicationSchema.reset
    }
  }
}