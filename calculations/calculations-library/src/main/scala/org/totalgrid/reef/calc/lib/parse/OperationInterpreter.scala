package org.totalgrid.reef.calc.lib.parse


object OperationInterpreter {

  case class Fun(fun: String, args: List[Expression]) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      ops.forName(fun).apply(args.map(_.evaluate(inputs, ops)))
    }
  }

  case class Infix(op: String, left: Expression, right: Expression) extends Expression {
    def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue = {
      ops.forName(op).apply(List(left.evaluate(inputs, ops), right.evaluate(inputs, ops)))
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
