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
package org.totalgrid.reef.httpbridge.servlets

import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito
import org.springframework.mock.web.{ MockHttpServletResponse, MockHttpServletRequest }
import org.scalatest.{ BeforeAndAfterEach, FunSuite }

import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.httpbridge._

abstract class BaseServletTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var connection: ManagedConnection = null
  var request: MockHttpServletRequest = null
  var response: MockHttpServletResponse = null
  val builderLocator = new BuilderLocator(new ReefServices)

  override def beforeEach() {
    connection = Mockito.mock(classOf[ManagedConnection], new MockitoStubbedOnly())
    request = new MockHttpServletRequest()
    response = new MockHttpServletResponse()
  }

  val userName = "system"
  val password = "sys-pass"

}