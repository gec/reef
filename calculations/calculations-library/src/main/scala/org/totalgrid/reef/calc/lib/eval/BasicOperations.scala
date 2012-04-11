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
      (List("SUM", "+"), () => new Sum),
      (List("SUBTRACT", "-"), () => new Subtract),
      (List("PRODUCT", "*"), () => new Product),
      (List("DIVIDE", "/"), () => new Divide),
      (List("POWER", "^"), () => new Power),
      (List("AVERAGE"), () => new Average),
      (List("MAX"), () => new Max),
      (List("MIN"), () => new Min),
      (List("GREATER"), () => new Greater),
      (List("LESS"), () => new Less),
      (List("SQRT"), () => new SquareRoot),
      (List("COUNT"), () => new Count),
      (List("AND"), () => new And),
      (List("OR"), () => new Or),
      (List("NOT"), () => new Not),
      (List("INTEGRATE"), () => new Integrate)))
  }

  class Sum extends MultiNumericOperation {
    def eval(args: List[Double]): Double = {
      args.foldLeft(0.0) { _ + _ }
    }
  }

  class Subtract extends PairNumericOperation {
    def eval(l: Double, r: Double): Double = { l - r }
  }

  class Product extends MultiNumericOperation {
    def eval(args: List[Double]): Double = {
      args.reduceLeft(_ * _)
    }
  }

  class Divide extends PairNumericOperation {
    def eval(l: Double, r: Double): Double = { l / r }
  }

  class Power extends PairNumericOperation {
    def eval(l: Double, r: Double): Double = { math.pow(l, r) }
  }

  class Average extends MultiNumericOperation {
    def eval(args: List[Double]): Double = {
      args.foldLeft(0.0) { _ + _ } / args.size
    }
  }

  class Max extends MultiNumericOperation {
    def eval(args: List[Double]): Double = {
      args.foldLeft(Option.empty[Double]) { (result, value) =>
        result match {
          case Some(last) => Some(if (last > value) last else value)
          case None => Some(value)
        }
      }.get
    }
  }

  class Min extends MultiNumericOperation {
    def eval(args: List[Double]): Double = {
      args.foldLeft(Option.empty[Double]) { (result, value) =>
        result match {
          case Some(last) => Some(if (last < value) last else value)
          case None => Some(value)
        }
      }.get
    }
  }

  class Greater extends ConditionalPairNumericOperation {
    def eval(l: Double, r: Double) = l > r
  }

  class Less extends ConditionalPairNumericOperation {
    def eval(l: Double, r: Double) = l < r
  }

  class SquareRoot extends SingleNumericOperation {
    def eval(v: Double): Double = math.sqrt(v)
  }

  class Not extends SingleBooleanOperation {
    def eval(arg: Boolean) = !arg
  }

  class And extends BooleanReduceOperation {
    def eval(args: List[Boolean]) = {
      args.foldLeft(true) { case (out, v) => out && v }
    }
  }

  class Or extends BooleanReduceOperation {
    def eval(args: List[Boolean]) = {
      args.foldLeft(false) { case (out, v) => out || v }
    }
  }

  class Count extends BooleanFoldOperation {
    def eval(args: List[Boolean]) = {
      args.foldLeft(0) { case (sum, v) => sum + (if (v) 1 else 0) }
    }
  }

  class Integrate extends AccumulatedNumericOperation {
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
