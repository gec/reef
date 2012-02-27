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
package org.totalgrid.reef.calc.lib.eval

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class OperationIntegrationTest extends FunSuite with ShouldMatchers {

  test("Simple math") {
    val f = "5 * 2"
    val expr = OperationParser.parseFormula(f)
    val result = NumericConst(10.0)

    expr.evaluate(new ValueMap(Map()), BasicOperations.getSource) should equal(result)
  }

  test("Average") {
    val f = "B + AVG(A)"
    val values = Map("B" -> 1.5, "A" -> ValueRange(List(NumericConst(5.0), NumericConst(10.0), NumericConst(15.0))))
    val expr = OperationParser.parseFormula(f)
    val result = NumericConst(11.5)

    expr.evaluate(new ValueMap(values), BasicOperations.getSource) should equal(result)
  }
}
