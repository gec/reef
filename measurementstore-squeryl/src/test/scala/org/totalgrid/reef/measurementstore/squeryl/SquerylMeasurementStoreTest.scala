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
package org.totalgrid.reef.measurementstore.squeryl

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.persistence.squeryl._
import net.agileautomata.executor4s.testing._
import org.totalgrid.reef.measurementstore._

trait SqlMeasStoreTestFixture {
  def connect(includeHistory: Boolean) = {
    val store = new SqlMeasurementStore({ () =>
      val conn_info = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg")
      DbConnector.connect(conn_info)
    }, includeHistory)
    store.connect()
    store.reset()
    store
  }
}

@RunWith(classOf[JUnitRunner])
class SqlMeasTest extends MeasurementStoreTest with SqlMeasStoreTestFixture {

  lazy val cm = connect(true)
}

@RunWith(classOf[JUnitRunner])
class SqlMeasRTDatabaseReadPerformanceTest extends RTDatabaseReadPerformanceTestBase with SqlMeasStoreTestFixture {

  lazy val cm = connect(true)
  def fname = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg").dbType + ".plt"
}

@RunWith(classOf[JUnitRunner])
class SqlMixedMeasTest extends MeasurementStoreTest with SqlMeasStoreTestFixture {

  lazy val cm = {
    val c = new MixedMeasurementStore(new MockExecutorService(new InstantExecutor()), new InMemoryMeasurementStore(false), connect(false))
    c.connect
    c
  }
}
