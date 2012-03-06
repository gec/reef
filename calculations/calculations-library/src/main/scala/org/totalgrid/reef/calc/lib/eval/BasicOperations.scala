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

import org.totalgrid.reef.calc.lib.eval.OperationPatterns._

object BasicOperations {

  def getSource = {
    new BasicOperationSource(List(
      new Sum,
      new Subtract,
      new Product,
      new Divide,
      new Power,
      new Average,
      new SquareRoot,
      new Count,
      new And,
      new Integrate))
  }

  class Sum extends MultiNumericOperation {
    def names = List("SUM", "+")

    def eval(args: List[Double]): Double = {
      args.foldLeft(0.0) { _ + _ }
    }
  }

  class Subtract extends PairNumericOperation {
    def names = List("SUB", "-")

    def eval(l: Double, r: Double): Double = { l - r }
  }

  class Product extends MultiNumericOperation {
    def names = List("PROD", "*")

    def eval(args: List[Double]): Double = {
      args.reduceLeft(_ * _)
    }
  }

  class Divide extends PairNumericOperation {
    def names = List("DIV", "/")

    def eval(l: Double, r: Double): Double = { l / r }
  }

  class Power extends PairNumericOperation {
    def names = List("POW", "^")

    def eval(l: Double, r: Double): Double = { math.pow(l, r) }
  }

  class Average extends MultiNumericOperation {
    def names = List("AVG")

    def eval(args: List[Double]): Double = {
      args.foldLeft(0.0) { _ + _ } / args.size
    }
  }

  class SquareRoot extends SingleNumericOperation {
    def names = List("SQRT")

    def eval(v: Double): Double = {
      math.sqrt(v)
    }
  }

  class And extends BooleanReduceOperation {
    def names = List("AND")

    def eval(args: List[Boolean]) = {
      args.foldLeft(true) { case (out, v) => if (out && v) true else false }
    }
  }

  class Count extends BooleanFoldOperation {
    def names = List("COUNT")

    def eval(args: List[Boolean]) = {
      args.foldLeft(0) { case (sum, v) => sum + (if (v) 1 else 0) }
    }
  }

  class Integrate extends AccumulatedNumericOperation {
    def names = List("INTEGRATE")

    def eval(initialValue: AccumulatedValue, args: List[NumericMeas]) = {
      args.foldLeft(initialValue.copy(value = 0)) {
        case (state, meas) =>
          state.lastMeas match {
            case Some(NumericMeas(v, t)) =>

              val time = (meas.time - t)
              if (time < 0) throw new EvalException("Measurements out of order. new: " + meas.time + " previous: " + t + " delta: " + time)
              val area = if (time > 0) ((meas.doubleValue + v) * time) / 2
              else 0
              state.copy(lastMeas = Some(meas), value = state.value + area)
            case None =>
              state.copy(lastMeas = Some(meas))
          }
      }
    }
  }
}
