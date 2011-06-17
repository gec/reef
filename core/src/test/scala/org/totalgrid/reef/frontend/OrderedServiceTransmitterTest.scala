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
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.japi.ServiceIOException
import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.sapi.{ AnyNodeDestination, Destination, RequestEnv }
import org.totalgrid.reef.sapi.client.{ Response, Subscription, ClientSession }
import org.totalgrid.reef.promise.{ FixedPromise, Promise }
import org.totalgrid.reef.sapi.client.SingleSuccess

@RunWith(classOf[JUnitRunner])
class OrderedServiceTransmitterTest extends FunSuite with ShouldMatchers {

  class EchoingClientSession extends ClientSession {

    var numRequests = 0
    private var open = true

    final override def isOpen = open
    final override def close() = open = false

    def addSubscription[A](klass: Class[_]): Subscription[A] = throw new ServiceIOException("Unimplemented")

    final override def request[A](verb: Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination): Promise[Response[A]] = {
      numRequests += 1
      new FixedPromise(SingleSuccess(single = payload))
    }

  }

  test("Construction") {
    val session = new EchoingClientSession
    val pool = new org.totalgrid.reef.messaging.mock.MockSessionPool(session)
    val ost = new OrderedServiceTransmitter(pool)

    ost.publish(4).await should equal(true)
  }

}
