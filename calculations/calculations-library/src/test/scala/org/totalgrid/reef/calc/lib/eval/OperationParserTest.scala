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
class OperationParserTest extends FunSuite with ShouldMatchers {
  import OperationInterpreter._

  test("Recursive Nesting") {

    val f = "5.5 + A * AVG(B, 5 + C, SUM(D, 2.2, 3))"

    val tree = Infix("+", ConstDouble(5.5), Infix("*", Var("A"), Fun("AVG", List(Var("B"), Infix("+", ConstLong(5), Var("C")), Fun("SUM", List(Var("D"), ConstDouble(2.2), ConstLong(3)))))))

    OperationParser.parseFormula(f) should equal(tree)
  }

  test("Precedence") {

    val f = "5 + 3 / 4 + 7.7"

    val tree = Infix("+", Infix("+", ConstLong(5), Infix("/", ConstLong(3), ConstLong(4))), ConstDouble(7.7))

    OperationParser.parseFormula(f) should equal(tree)
  }

  test("Literal Handling") {

    val f = "(5 + 5.5) + (true + false)"

    val tree = Infix("+", Infix("+", ConstLong(5), ConstDouble(5.5)), Infix("+", ConstBoolean(true), ConstBoolean(false)))

    OperationParser.parseFormula(f) should equal(tree)
  }

}
