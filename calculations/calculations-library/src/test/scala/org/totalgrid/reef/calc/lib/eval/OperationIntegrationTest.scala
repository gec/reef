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
    val f = "B + AVERAGE(A)"
    val expr = parseFormula(f)
    val result = NumericConst(11.5)

    val doubleValues = Map("B" -> 1.5, "A" -> ValueRange(List(NumericConst(5.0), NumericConst(10.0), NumericConst(15.0))))
    expr.evaluate(new ValueMap(doubleValues)) should equal(result)

    // show that math works when we pass in Longs and cast them up to doubles
    val longValues = Map("B" -> 1.5, "A" -> ValueRange(List(LongConst(5), LongConst(10), LongConst(15))))
    expr.evaluate(new ValueMap(longValues)) should equal(result)
  }

  test("Numeric MAX") {

    val tests = List(
      (5.0, List(5.0)),
      (10.0, List(10.0, 5.0)),
      (-10.0, List(-10.0, -50.0)),
      (50.0, List(50.0, 49.9, 49.8)))

    testNumeric("MAX(A)", tests)
  }

  test("Numeric MIN") {

    val tests = List(
      (5.0, List(5.0)),
      (5.0, List(10.0, 5.0)),
      (-50.0, List(-10.0, -50.0)),
      (49.8, List(50.0, 49.9, 49.8)))

    testNumeric("MIN(A)", tests)
  }

  test("Boolean AND") {

    val tests = List(
      ("AND(true)", BooleanConst(true)),
      ("AND(false)", BooleanConst(false)),
      ("AND(true,true)", BooleanConst(true)),
      ("AND(true,false)", BooleanConst(false)),
      ("AND(false,false)", BooleanConst(false)))

    testWithoutValues(tests)
  }

  test("Boolean OR") {

    val tests = List(
      ("OR(true)", BooleanConst(true)),
      ("OR(false)", BooleanConst(false)),
      ("OR(true,true)", BooleanConst(true)),
      ("OR(true,false)", BooleanConst(true)),
      ("OR(false,false)", BooleanConst(false)))

    testWithoutValues(tests)
  }

  test("Boolean NOT") {

    val tests = List(
      ("NOT(true)", BooleanConst(false)),
      ("NOT(false)", BooleanConst(true)))

    testWithoutValues(tests)

    val errorTests = List(
      ("NOT(true,true)", "requires one"))

    testErrorsWithoutValues(errorTests)
  }

  test("Boolean COUNT") {

    val tests = List(
      ("COUNT(true)", LongConst(1)),
      ("COUNT(false)", LongConst(0)),
      ("COUNT(true,true)", LongConst(2)),
      ("COUNT(true,false)", LongConst(1)),
      ("COUNT(false,false)", LongConst(0)))

    testWithoutValues(tests)
  }

  test("Boolean GREATER") {

    val tests = List(
      ("GREATER(0,0)", BooleanConst(false)),
      ("GREATER(1,0)", BooleanConst(true)),
      ("GREATER(5.5,5.4)", BooleanConst(true)),
      ("GREATER(-19,-25)", BooleanConst(true)),
      ("GREATER(-25,-20)", BooleanConst(false)),
      ("GREATER(4,7)", BooleanConst(false)))

    testWithoutValues(tests)
  }

  test("Boolean LESS") {

    val tests = List(
      ("LESS(0,0)", BooleanConst(false)),
      ("LESS(1,0)", BooleanConst(false)),
      ("LESS(5.5,5.4)", BooleanConst(false)),
      ("LESS(-19,-25)", BooleanConst(false)),
      ("LESS(-25,-20)", BooleanConst(true)),
      ("LESS(4,7)", BooleanConst(true)))

    testWithoutValues(tests)
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
    val expr = new AccumulatedFormula(NumericConst(0), parseFormula(f))

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

  private def testErrorsWithoutValues(errorTests: List[(String, String)]) {
    errorTests.foreach {
      case (f, errString) =>
        val expr = parseFormula(f)
        intercept[EvalException] {
          expr.evaluate(new ValueMap(Map()))
        }.getMessage should include(errString)
    }
  }

  private def testWithoutValues(tests: List[(String, OperationValue)]) {
    tests.foreach {
      case (f, result) =>
        val expr = parseFormula(f)
        expr.evaluate(new ValueMap(Map())) should equal(result)
    }
  }

  private def testNumeric(f: String, tests: List[(Double, List[Double])]) {
    tests.foreach {
      case (output, inputs) =>
        val values = Map("A" -> ValueRange(inputs.map { NumericConst(_) }))
        val result = NumericConst(output)
        val expr = parseFormula(f)
        expr.evaluate(new ValueMap(values)) should equal(result)
    }
  }
}
