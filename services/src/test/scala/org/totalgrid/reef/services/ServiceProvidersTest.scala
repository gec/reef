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

import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.services.framework.{ ServiceContainer, ServerSideProcess }
import org.totalgrid.reef.clientapi.settings.{ UserSettings, NodeSettings }
import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.util.Lifecycle
import org.totalgrid.reef.clientapi.sapi.client.rest.Connection
import org.totalgrid.reef.clientapi.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.clientapi.sapi.service.{ ServiceResponseCallback, AsyncService }
import org.totalgrid.reef.clientapi.proto.Envelope
import org.totalgrid.reef.clientapi.types.TypeDescriptor
import org.totalgrid.reef.services.authz.NullAuthService

/**
 * A concrete example service that always responds immediately with Success and the correct Id
 */
class NoOpService extends AsyncService[Any] {

  import Envelope._

  /// noOpService that returns OK
  def respond(request: ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) =
    callback.onResponse(ServiceResponse.newBuilder.setStatus(Status.OK).setId(request.getId).build)

  override val descriptor = new TypeDescriptor[Any] {
    def serialize(typ: Any): Array[Byte] = throw new Exception("unimplemented")
    def deserialize(data: Array[Byte]): Any = throw new Exception("unimplemented")
    def getKlass: Class[Any] = throw new Exception("unimplemented")
    def id = "Any"
  }
}

@RunWith(classOf[JUnitRunner])
class ServiceProvidersTest extends DatabaseUsingTestBase {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("../org.totalgrid.reef.test.cfg"))
  }

  class ExchangeCheckingServiceContainer(amqp: Connection) extends ServiceContainer {
    def addCoordinator(coord: ServerSideProcess) {}

    def addLifecycleObject(obj: Lifecycle) {}

    def attachService(endpoint: AsyncService[_]): AsyncService[_] = {
      val klass = endpoint.descriptor.getKlass
      //call just so an exception will be thrown if it doesn't exist
      amqp.declareEventExchange(klass)
      new NoOpService
    }
  }

  test("All Service Providers are in services list") {
    ConnectionFixture.mock() { amqp =>
      ServiceBootstrap.resetDb
      ServiceBootstrap.seed("system")

      val userSettings = new UserSettings("system", "system")
      val nodeSettings = new NodeSettings("node1", "network", "location")
      val serviceOptions = ServiceOptions.fromFile("../org.totalgrid.reef.test.cfg")

      val components = ServiceBootstrap.bootstrapComponents(amqp, userSettings, nodeSettings)
      val measStore = new InMemoryMeasurementStore
      val serviceContainer = new ExchangeCheckingServiceContainer(amqp)
      val metrics = MetricsSink.getInstance("test")

      val provider = new ServiceProviders(amqp, measStore, serviceOptions, NullAuthService, metrics, "")
      serviceContainer.addCoordinator(provider.coordinators)
      serviceContainer.attachServices(provider.services)
    }
  }

}