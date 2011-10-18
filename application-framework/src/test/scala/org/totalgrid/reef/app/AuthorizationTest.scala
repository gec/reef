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
package org.totalgrid.reef.app

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.messaging.mock.synchronous.MockConnection
import org.totalgrid.reef.executor.mock.InstantExecutor

import org.scalatest.{ BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.api.proto.Auth.AuthToken
import org.junit.runner.RunWith
import org.totalgrid.reef.sapi.client.{ Response, FailureResponse }
import org.totalgrid.reef.japi.Envelope
import scala.Some

@RunWith(classOf[JUnitRunner])
class AuthorizationTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var env: Option[String] = None
  var conn = new MockConnection
  var exe = new InstantExecutor

  override def beforeEach() = {
    env = None
    conn = new MockConnection
    exe = new InstantExecutor
    Authorization.login(conn.getSessionPool, exe, 200)(x => env = Some(x.getToken))
  }

  test("Success") {

    conn.session.numRequestsPending should equal(1)

    conn.session.respond[AuthToken] { request =>
      val rsp = AuthToken.newBuilder(request.payload).setToken("magic").build
      Response(Envelope.Status.OK, rsp)
    }

    conn.session.numRequestsPending should equal(0)

    env should equal(Some("magic"))
  }

  test("Retry on failure") {

    conn.session.numRequestsPending should equal(1)

    conn.session.respond[AuthToken] { request =>
      FailureResponse(Envelope.Status.INTERNAL_ERROR)
    }

    conn.session.numRequestsPending should equal(1)

    env should equal(None)
  }

}