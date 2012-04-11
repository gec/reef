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

object OperationInterpreter {

  private def getOrElse(ops: OperationSource, name: String): Operation = {
    ops.forName(name).getOrElse { throw new EvalException("Operation not found: " + name) }
  }

  case class Fun(fun: String, args: List[Expression]) extends Expression {
    def prepare(ops: OperationSource): PreparedExpression = {
      PreparedFun(getOrElse(ops, fun), args.map { _.prepare(ops) })
    }
  }

  case class PreparedFun(operation: Operation, args: List[PreparedExpression]) extends PreparedExpression {
    def evaluate(inputs: VariableSource) = {
      operation.apply(args.map(_.evaluate(inputs)).flatMap(_.toList))
    }
  }

  case class Infix(op: String, left: Expression, right: Expression) extends Expression {
    def prepare(ops: OperationSource): PreparedExpression = {
      PreparedInfix(getOrElse(ops, op), left.prepare(ops), right.prepare(ops))
    }
  }

  case class PreparedInfix(operation: Operation, left: PreparedExpression, right: PreparedExpression) extends PreparedExpression {
    def evaluate(inputs: VariableSource) = {
      operation.apply(List(left.evaluate(inputs), right.evaluate(inputs)).flatMap(_.toList))
    }
  }

  trait SimpleExpr extends Expression with PreparedExpression {
    def prepare(ops: OperationSource) = this
  }

  case class ConstDouble(v: Double) extends SimpleExpr {
    def evaluate(inputs: VariableSource): OperationValue = {
      NumericConst(v)
    }
  }

  case class ConstLong(v: Long) extends SimpleExpr {
    def evaluate(inputs: VariableSource): OperationValue = {
      LongConst(v)
    }
  }

  case class ConstBoolean(v: Boolean) extends SimpleExpr {
    def evaluate(inputs: VariableSource): OperationValue = {
      BooleanConst(v)
    }
  }

  case class Var(name: String) extends SimpleExpr {
    def evaluate(inputs: VariableSource): OperationValue = {
      inputs.forName(name)
    }
  }

}
