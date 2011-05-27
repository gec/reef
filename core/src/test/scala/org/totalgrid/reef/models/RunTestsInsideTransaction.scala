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

import org.scalatest.FunSuite
import org.scalatest.{ Reporter, Stopper, Filter, Distributor, Tracker }

import org.squeryl.PrimitiveTypeMode.transaction

trait RunTestsInsideTransaction extends FunSuite {

  case object TransactionAbortException extends Exception

  override def runTest(
    testName: String,
    reporter: Reporter,
    stopper: Stopper,
    configMap: Map[String, Any],
    tracker: Tracker): Unit = {

    try {
      // each test occur from within a transaction, that way when the test completes _all_ changes
      // made during the test are reverted so each test gets a clean enviroment to test against
      transaction {
        super.runTest(testName, reporter, stopper, configMap, tracker)

        // we abort the transaction if we get to here, so changes get rolled back
        throw TransactionAbortException
      }
    } catch {
      case TransactionAbortException =>
    }
  }
}