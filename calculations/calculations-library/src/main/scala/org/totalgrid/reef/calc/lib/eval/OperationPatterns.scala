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

object OperationPatterns {
  trait AbstractOperation extends Operation {
    protected val name = names.headOption.getOrElse(this.getClass.getSimpleName)
  }

  trait MultiNumericOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      if (args.size > 0) {
        val nums = args.map {
          case NumericValue(v) => v
          case _ => throw new EvalException("Operation " + name + " only takes numeric values")
        }
        NumericConst(eval(nums))
      } else {
        throw new EvalException("Operation " + name + " requires one or more value")
      }
    }

    def eval(args: List[Double]): Double
  }

  trait PairNumericOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      args match {
        case List(NumericValue(l), NumericValue(r)) => NumericConst(eval(l, r))
        case _ => throw new EvalException("Operation " + name + " requires exactly two numeric values")
      }
    }

    def eval(l: Double, r: Double): Double
  }

  trait ConditionalPairNumericOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      args match {
        case List(NumericValue(l), NumericValue(r)) => BooleanConst(eval(l, r))
        case _ => throw new EvalException("Operation " + name + " requires exactly two numeric values")
      }
    }

    def eval(l: Double, r: Double): Boolean
  }

  trait SingleNumericOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      args match {
        case List(NumericValue(v)) => NumericConst(eval(v))
        case _ => throw new EvalException("Operation " + name + " requires one numeric value")
      }
    }

    def eval(v: Double): Double
  }

  abstract class BooleanFoldOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      if (args.size > 0) {
        val vals = args.map {
          case BooleanValue(v) => v
          case _ => throw new EvalException("Operation " + name + " only takes boolean values")
        }
        LongConst(eval(vals))
      } else {
        throw new EvalException("Operation " + name + " requires one or more value")
      }
    }

    def eval(args: List[Boolean]): Int
  }

  abstract class SingleBooleanOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      args match {
        case List(BooleanValue(v)) => BooleanConst(eval(v))
        case _ => throw new EvalException("Operation " + name + " requires one boolean value")
      }
    }

    def eval(arg: Boolean): Boolean
  }

  abstract class BooleanReduceOperation extends AbstractOperation {
    def apply(args: List[OperationValue]): OperationValue = {
      if (args.size > 0) {
        val nums = args.map {
          case BooleanValue(v) => v
          case _ => throw new EvalException("Operation " + name + " only takes boolean values")
        }
        BooleanConst(eval(nums))
      } else {
        throw new EvalException("Operation " + name + " requires one or more value")
      }
    }

    def eval(args: List[Boolean]): Boolean
  }

  trait AccumulatedNumericOperation extends AbstractOperation {

    case class AccumulatedValue(value: Double, lastMeas: Option[NumericMeas])

    var accumulatedValue = AccumulatedValue(0, None)

    def apply(args: List[OperationValue]): OperationValue = {
      if (args.size > 0) {
        val nums = args.map {
          case NumericMeas(v, t) => NumericMeas(v, t)
          case _ => throw new EvalException("Operation " + name + " only takes numeric values")
        }
        accumulatedValue = eval(accumulatedValue, nums)
        NumericConst(accumulatedValue.value)
      } else {
        NumericConst(accumulatedValue.value)
      }
    }

    def eval(initialValue: AccumulatedValue, args: List[NumericMeas]): AccumulatedValue
  }
}
