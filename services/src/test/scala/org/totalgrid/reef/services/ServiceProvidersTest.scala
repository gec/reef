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

import org.totalgrid.reef.executor.Lifecycle
import org.totalgrid.reef.sapi.auth.NullAuthService
import org.totalgrid.reef.sapi.service.{ NoOpService, AsyncService }

import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.proto.{ ReefServicesList }

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.executor.mock.InstantExecutor
import org.totalgrid.reef.app.ApplicationEnroller

@RunWith(classOf[JUnitRunner])
class ServiceProvidersTest extends DatabaseUsingTestBase {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }

  class ExchangeCheckingServiceContainer extends ServiceContainer {
    def addCoordinator(coord: ProtoServiceCoordinator) {}

    def addLifecycleObject(obj: Lifecycle) {}

    def attachService(endpoint: AsyncService[_]): AsyncService[_] = {
      ReefServicesList.getServiceInfo(endpoint.descriptor.getKlass) //call just so an exception will be thrown if it doesn't exist
      new NoOpService
    }
  }

  test("All Service Providers are in services list") {
    AMQPFixture.mock(true) { amqp =>
      ServiceBootstrap.resetDb
      ServiceBootstrap.seed

      val userSettings = ApplicationEnroller.getDefaultUserSettings
      val nodeSettings = ApplicationEnroller.getDefaultNodeSettings

      val components = ServiceBootstrap.bootstrapComponents(amqp, userSettings, nodeSettings)
      val measStore = new InMemoryMeasurementStore
      val serviceContainer = new ExchangeCheckingServiceContainer

      val serviceOptions = ServiceOptions.loadInfo

      val provider = new ServiceProviders(components, measStore, serviceOptions, NullAuthService, new InstantExecutor)
      serviceContainer.addCoordinator(provider.coordinators)
      serviceContainer.attachServices(provider.services)
    }
  }

}