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
package org.totalgrid.reef.services

import org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor
import org.totalgrid.reef.client.service.proto.Auth.{ AuthToken, Agent }

import org.totalgrid.reef.services.framework.{ RequestContext, RequestContextSource }
import org.totalgrid.reef.client.settings.{ UserSettings, NodeSettings }
import org.totalgrid.reef.client.Connection
import org.totalgrid.reef.client.sapi.rpc.impl.builders.ApplicationConfigBuilders
import org.totalgrid.reef.services.core.{ ModelFactories, ApplicationConfigService, AuthTokenService, FrontEndProcessorService }
import org.totalgrid.reef.client.service.list.ReefServicesList
import org.totalgrid.reef.event.SilentEventSink
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.persistence.squeryl.DbConnection
import org.totalgrid.reef.services.authz.SqlAuthzService
import org.totalgrid.reef.services.settings.Version

import org.totalgrid.reef.client.sapi.client.Expectations._

object ServiceBootstrap {

  def buildLogin(userSettings: UserSettings): AuthToken = {
    val agent = Agent.newBuilder
    agent.setName(userSettings.getUserName).setPassword(userSettings.getUserPassword)
    val auth = AuthToken.newBuilder
    auth.setAgent(agent)
    auth.setClientVersion(Version.getClientVersion)
    auth.build
  }

  /**
   * when we are starting up the system we need to define all of the event exchanges, we do that
   * during bootstrap so we correctly publish the "someone logged on" events
   */
  def defineEventExchanges(connection: Connection) {
    import scala.collection.JavaConversions._
    ReefServicesList.getServicesList.toList.foreach { serviceInfo =>
      connection.getServiceRegistration.declareEventExchange(serviceInfo.getDescriptor.getKlass)
    }
  }

  /**
   * since _we_are_ a service provider we can create whatever services we would normally
   * use to enroll ourselves as an application to get the CoreApplicationComponents without
   * repeating that setup logic somewhere else
   */
  def bootstrapComponents(dbConnection: DbConnection, connection: Connection, systemUser: UserSettings, appSettings: NodeSettings) = {
    val dependencies = new RequestContextDependencies(dbConnection, connection, connection.getServiceRegistration.getEventPublisher, "", new SilentEventSink, new SqlAuthzService())

    // define the events exchanges before "logging in" which will generate some events
    defineEventExchanges(connection)

    val contextSource = new DependenciesSource(dependencies)
    val modelFac = new ModelFactories(new InMemoryMeasurementStore, contextSource)
    val applicationConfigService = new ApplicationConfigService(modelFac.appConfig)
    val fepService = new FrontEndProcessorService(modelFac.fep)
    val authService = new AuthTokenService(modelFac.authTokens)

    val login = buildLogin(systemUser)
    val authToken = authService.put(contextSource, login).expectOne

    // since we aren't using the full service middleware we need to manually do the auth step
    val authorizedSource = new RequestContextSource {
      def transaction[A](f: (RequestContext) => A) = {
        contextSource.transaction { context =>
          context.modifyHeaders(_.setAuthToken(authToken.getToken))
          (new SqlAuthzService()).prepare(context)
          f(context)
        }
      }
    }

    val config = ApplicationConfigBuilders.makeProto(Version.getClientVersion, appSettings, appSettings.getDefaultNodeName + "-Services", List("Services"))
    val appConfig = applicationConfigService.put(authorizedSource, config).expectOne

    // the measurement batch service acts as a type of manual FEP
    val fepConfig = FrontEndProcessor.newBuilder.setAppConfig(appConfig).addProtocols("null").build

    fepService.put(authorizedSource, fepConfig)

    (appConfig, authToken.getToken)
  }

  /**
   * sets up the default users and low level configurations for the system
   */
  def seed(dbConnection: DbConnection, systemPassword: String) {
    val context = new SilentRequestContext
    dbConnection.transaction {
      ServiceSeedData.seed(context, systemPassword)
    }
  }

}