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
import org.totalgrid.reef.persistence.squeryl.{ DbConnection, DbInfo, DbConnector }
import org.totalgrid.reef.util.Timing
import com.weiglewilczek.slf4s.Logging

object ConnectionStorage {
  private var lastConnection = Option.empty[DbConnection]
  private var lastOptions = Option.empty[DbInfo]

  def connect(dbInfo: DbInfo): DbConnection = {
    if (lastOptions != Some(dbInfo)) {
      lastConnection = Some(DbConnector.connect(dbInfo))
      lastOptions = Some(dbInfo)
    }
    lastConnection.get
  }

  var dbNeedsReset = true
}

abstract class DatabaseUsingTestBaseNoTransaction extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with Logging {
  lazy val dbConnection = ConnectionStorage.connect(DbInfo.loadInfo("../../org.totalgrid.reef.test.cfg"))

  protected def alwaysReset = true

  override def beforeAll() {
    if (ConnectionStorage.dbNeedsReset || alwaysReset) {
      val prepareTime = Timing.benchmark {
        CoreServicesSchema.prepareDatabase(dbConnection, true, false)
      }
      logger.info("Prepared db in: " + prepareTime)
      ConnectionStorage.dbNeedsReset = false
    }
  }

  /**
   * we only need to rebuild the database schema when a Non-Transaction-Safe test suite has been run.
   */
  protected def resetDbAfterTestSuite: Boolean
  override def afterAll() {
    ConnectionStorage.dbNeedsReset = resetDbAfterTestSuite
  }
}

abstract class DatabaseUsingTestBase extends DatabaseUsingTestBaseNoTransaction with RunTestsInsideTransaction {
  protected override def resetDbAfterTestSuite = false
}

abstract class DatabaseUsingTestNotTransactionSafe extends DatabaseUsingTestBaseNoTransaction {
  protected override def resetDbAfterTestSuite = true
}