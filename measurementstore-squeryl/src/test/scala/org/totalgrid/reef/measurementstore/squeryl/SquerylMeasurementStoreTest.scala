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

import org.totalgrid.reef.measurementstore.MeasurementStoreTest
import org.totalgrid.reef.measurementstore.RTDatabaseReadPerformanceTestBase

import org.totalgrid.reef.persistence.squeryl._
import org.squeryl.PrimitiveTypeMode._
import postgresql.PostgresqlReset

@RunWith(classOf[JUnitRunner])
class SqlMeasTest extends MeasurementStoreTest {
  def connect() = {

    val conn_info = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg")
    val connection = DbConnector.connect(conn_info)
    PostgresqlReset.reset()
    transaction { SqlMeasurementStoreSchema.reset() }
    SqlMeasurementStore
  }
  lazy val cm = connect()
}

@RunWith(classOf[JUnitRunner])
class SqlMeasRTDatabaseReadPerformanceTest extends RTDatabaseReadPerformanceTestBase {

  def connect() = {
    import org.totalgrid.reef.persistence.squeryl._
    val conn_info = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg")
    val connection = DbConnector.connect(conn_info)
    PostgresqlReset.reset()
    val store = SqlMeasurementStore
    store.reset
    store
  }
  lazy val cm = connect()
  def fname = DbInfo.loadInfo("../org.totalgrid.reef.test.cfg").dbType + ".plt"
}
