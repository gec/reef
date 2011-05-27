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
package org.totalgrid.reef.persistence.squeryl

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.measurementstore.MeasurementStoreTest
import org.totalgrid.reef.measurementstore.RTDatabaseReadPerformanceTestBase

import org.totalgrid.reef.persistence.squeryl._
import org.totalgrid.reef.persistence.LockStepConnection
import org.squeryl.PrimitiveTypeMode._
import postgresql.PostgresqlReset

@RunWith(classOf[JUnitRunner])
class SqlMeasTest extends MeasurementStoreTest {
  def connect() = {

    val conn_info = DbInfo.loadInfo("test")
    val connection = DbConnector.connect(conn_info)
    PostgresqlReset.reset()
    transaction { SqlMeasurementStoreSchema.reset() }
    new SqlMeasurementStore(new LockStepConnection(true))
  }
  lazy val cm = connect()
}

@RunWith(classOf[JUnitRunner])
class SqlMeasRTDatabaseReadPerformanceTest extends RTDatabaseReadPerformanceTestBase {

  def connect() = {
    import org.totalgrid.reef.persistence.squeryl._
    import org.totalgrid.reef.persistence.LockStepConnection
    val conn_info = DbInfo.loadInfo("test")
    val connection = DbConnector.connect(conn_info)
    import org.squeryl.PrimitiveTypeMode._
    PostgresqlReset.reset()
    val store = new SqlMeasurementStore(new LockStepConnection(true))
    store.reset
    store
  }
  lazy val cm = connect()
  def fname = DbInfo.loadInfo("test").dbType + ".plt"
}
