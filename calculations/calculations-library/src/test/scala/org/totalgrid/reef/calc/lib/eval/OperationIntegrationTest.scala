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

  def parseFormula(f: String): Formula = {
    Formula(OperationParser.parseFormula(f), BasicOperations.getSource)
  }

  test("Simple math") {
    val f = "5 * 2"
    val expr = parseFormula(f)
    val result = NumericConst(10.0)

    expr.evaluate(new ValueMap(Map())) should equal(result)
  }

  test("Average") {
    val f = "B + AVG(A)"
    val expr = parseFormula(f)
    val result = NumericConst(11.5)

    val doubleValues = Map("B" -> 1.5, "A" -> ValueRange(List(NumericConst(5.0), NumericConst(10.0), NumericConst(15.0))))
    expr.evaluate(new ValueMap(doubleValues)) should equal(result)

    // show that math works when we pass in Longs and cast them up to doubles
    val longValues = Map("B" -> 1.5, "A" -> ValueRange(List(LongConst(5), LongConst(10), LongConst(15))))
    expr.evaluate(new ValueMap(longValues)) should equal(result)
  }

  test("Boolean AND") {

    val tests = List(
      ("AND(true)", BooleanConst(true)),
      ("AND(false)", BooleanConst(false)),
      ("AND(true,true)", BooleanConst(true)),
      ("AND(true,false)", BooleanConst(false)),
      ("AND(false,false)", BooleanConst(false)))

    tests.foreach {
      case (f, result) =>
        val expr = parseFormula(f)
        expr.evaluate(new ValueMap(Map())) should equal(result)
    }
  }

  test("Boolean COUNT") {

    val tests = List(
      ("COUNT(true)", LongConst(1)),
      ("COUNT(false)", LongConst(0)),
      ("COUNT(true,true)", LongConst(2)),
      ("COUNT(true,false)", LongConst(1)),
      ("COUNT(false,false)", LongConst(0)))

    tests.foreach {
      case (f, result) =>
        val expr = parseFormula(f)
        expr.evaluate(new ValueMap(Map())) should equal(result)
    }
  }

  test("Numeric INTEGRATE") {

    val f = "INTEGRATE(A)"

    val tests = List(
      (20 * 5.0, List((5.0, 0), (5.0, 10), (5.0, 20))),
      (0.0, List((0.0, 0), (0.0, 10), (0.0, 20))),
      (100.0, List((0.0, 0), (5.0, 10), (10.0, 20))),
      (100.0, List((10.0, 0), (5.0, 10), (0.0, 20))),
      (300.0, List((10.0, 0), (10.0, 10), (20.0, 10), (20.0, 20))))

    tests.foreach {
      case (output, inputs) =>
        val values = Map("A" -> ValueRange(inputs.map { v => NumericMeas(v._1, v._2) }))
        val result = NumericConst(output)
        val expr = parseFormula(f)
        expr.evaluate(new ValueMap(values)) should equal(result)
    }
  }

  test("Numeric INTEGRATE errors") {

    val f = "INTEGRATE(A)"

    val tests = List(
      ("one or more", List()),
      ("out of order", List((0.0, 10), (0.0, 0))))

    tests.foreach {
      case (errString, inputs) =>
        val values = Map("A" -> ValueRange(inputs.map { v => NumericMeas(v._1, v._2) }))
        val expr = parseFormula(f)
        intercept[EvalException] {
          expr.evaluate(new ValueMap(values))
        }.getMessage should include(errString)
    }
  }

  test("Numeric INTEGRATE (Accumulated)") {

    val f = "INTEGRATE(A)"
    val expr = parseFormula(f)

    val tests = List(
      (10.0, List((5.0, 0), (5.0, 1), (5.0, 2))),
      (30.0, List((10.0, 2), (10.0, 3), (10.0, 4))),
      (30.0, List((20.0, 4))),
      (50.0, List((20.0, 5))))

    tests.foreach {
      case (output, inputs) =>
        val values = Map("A" -> ValueRange(inputs.map { v => NumericMeas(v._1, v._2) }))
        val result = NumericConst(output)
        expr.evaluate(new ValueMap(values)) should equal(result)
    }
  }
}
