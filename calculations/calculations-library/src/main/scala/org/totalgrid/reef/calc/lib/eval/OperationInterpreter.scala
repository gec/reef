package org.totalgrid.reef.calc.lib.eval
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

object OperationInterpreter {

  case class Fun(fun: String, args: List[Expression]) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      ops.forName(fun).apply(args.map(_.evaluate(inputs, ops)).flatMap(_.toList))
    }
  }

  case class Infix(op: String, left: Expression, right: Expression) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      ops.forName(op).apply(List(left.evaluate(inputs, ops), right.evaluate(inputs, ops)).flatMap(_.toList))
    }
  }

  case class Const(v: Double) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      NumericValue(v)
    }
  }

  case class Var(name: String) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      inputs.forName(name)
    }
  }

}
