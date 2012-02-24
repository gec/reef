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
package org.totalgrid.reef.calc.lib.parse

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class OperationInterpreterTest extends FunSuite with ShouldMatchers {
  import OperationInterpreter._

  class ValueMap(map: Map[String, OperationValue]) extends VariableSource {
    def forName(name: String): OperationValue = map(name)
  }
  class OpMap(map: Map[String, Operation]) extends OperationSource {
    def forName(name: String): Operation = map(name)
  }

  class Op(n: String, f: List[OperationValue] => OperationValue) extends Operation {
    def name: String = n
    def apply(inputs: List[OperationValue]): OperationValue = f(inputs)
  }

  test("FunTest") {

    val exp = Fun("SUM", List(Var("A"), Var("B"), Const(5.0)))

    val ins = Map("A" -> NumericValue(2.0), "B" -> NumericValue(3.0))

    def check(inputs: List[OperationValue]): OperationValue = inputs match {
      case List(NumericValue(2.0), NumericValue(3.0), NumericValue(5.0)) => NumericValue(10.0)
      case _ => throw new Exception("not matching")
    }

    val ops = new OpMap(Map("SUM" -> new Op("SUM", check)))

    exp.evaluate(new ValueMap(ins), ops) should equal(NumericValue(10.0))
  }

  test("Nesting") {

    val exp = Fun("SUM", List(Var("A"), Var("B"), Infix("*", Var("C"), Var("D"))))

    val ins = Map(
      "A" -> NumericValue(2.0),
      "B" -> NumericValue(3.0),
      "C" -> NumericValue(5.0),
      "D" -> NumericValue(7.0))

    def sum(inputs: List[OperationValue]): OperationValue = inputs match {
      case List(NumericValue(2.0), NumericValue(3.0), NumericValue(35.0)) => NumericValue(40.0)
    }
    def mult: List[OperationValue] => OperationValue = {
      case List(NumericValue(5.0), NumericValue(7.0)) => NumericValue(35.0)
    }

    val ops = new OpMap(Map("SUM" -> new Op("SUM", sum), "*" -> new Op("*", mult)))

    exp.evaluate(new ValueMap(ins), ops) should equal(NumericValue(40.0))
  }
}
