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
package org.totalgrid.reef.models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }
import org.totalgrid.reef.services.ServiceBootstrap
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.services.core.{ SyncService, ServiceDependenciesDefaults, MockRequestContextSource }
import org.totalgrid.reef.services.framework.{ RequestContextSource, ServiceEntryPoint }

abstract class DatabaseUsingTestBaseNoTransaction extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {
  lazy val dbConnection = DbConnector.connect(DbInfo.loadInfo("../org.totalgrid.reef.test.cfg"))
  override def beforeAll() {
    ServiceBootstrap.resetDb(dbConnection)
  }

  // TODO: move defaultContextSource and ServiceDependenciesDefaults to trait the tests can include
  def getRequestContextSource() = {
    val headers = BasicRequestHeaders.empty.setUserName("user")
    new MockRequestContextSource(new ServiceDependenciesDefaults(dbConnection), headers)
  }
  lazy val defaultContextSource = getRequestContextSource()
  def sync[A <: AnyRef](service: ServiceEntryPoint[A]): SyncService[A] = {
    sync(service, defaultContextSource)
  }
  def sync[A <: AnyRef](service: ServiceEntryPoint[A], contextSource: RequestContextSource): SyncService[A] = {
    new SyncService(service, contextSource)
  }
}

abstract class DatabaseUsingTestBase extends DatabaseUsingTestBaseNoTransaction with RunTestsInsideTransaction {

}