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
package org.totalgrid.reef.frontend

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito

import org.totalgrid.reef.executor.mock.MockExecutor
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.test.MockitoStubbedOnly
import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.promise.{ FixedPromise, Promise }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection, FrontEndProcessor }
import org.totalgrid.reef.japi.client.SubscriptionResult
import org.totalgrid.reef.japi.BadRequestException

import FrontEndTestHelpers._

@RunWith(classOf[JUnitRunner])
class FrontEndManagerTest extends FunSuite with ShouldMatchers {

  val applicationUuid: ReefUUID = "0"
  val protocolList = List("mock")

  def fixture(services: FrontEndProviderServices, autoStart: Boolean = true)(test: (FrontEndProviderServices, MockExecutor, MockSubscriptionHandler[CommEndpointConnection], FrontEndManager) => Unit) = {

    val exe = new MockExecutor

    val mp = new MockSubscriptionHandler[CommEndpointConnection]
    val appConfig = ApplicationConfig.newBuilder.setUuid(applicationUuid).build
    val fem = new FrontEndManager(services, exe, mp, appConfig, protocolList, 5000)

    if (autoStart) fem.start()
    test(services, exe, mp, fem)
  }

  def responses(fepResult: => Promise[FrontEndProcessor], subResult: => Promise[SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]]) = {
    val services = Mockito.mock(classOf[FrontEndProviderServices], new MockitoStubbedOnly)

    Mockito.doReturn(fepResult).when(services).registerApplicationAsFrontEnd(applicationUuid, protocolList)
    Mockito.doReturn(subResult).when(services).subscribeToEndpointConnectionsForFrontEnd(fepResult.await)

    services
  }

  test("Announces on startup") {
    val fep = new FixedPromise(FrontEndProcessor.newBuilder.setUuid("someuid").build)
    val sub = new FixedPromise(new MockSubscriptionResult[CommEndpointConnection](Nil))
    fixture(responses(fep, sub), false) { (services, exe, mp, fem) =>
      fem.start()
      mp.sub should equal(Some(sub.await))
      mp.canceled should equal(false)
      fem.stop()
      mp.canceled should equal(true)
    }
  }

  test("Retries announces with executor delay") {
    val fep = new FixedPromise(FrontEndProcessor.newBuilder.setUuid("someuid").build)
    val sub = new ThrowsPromise(new BadRequestException("Intentional Failure"))
    fixture(responses(fep, sub), false) { (services, exe, mp, fem) =>
      fem.start()
      mp.sub should equal(None)
      (0 to 3).foreach { i =>
        exe.delayNext(1, 1) should equal(5000)
      }
    }
  }
}