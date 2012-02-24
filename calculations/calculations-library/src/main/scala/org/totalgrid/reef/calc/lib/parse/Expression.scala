package org.totalgrid.reef.calc.lib.parse

import org.totalgrid.reef.client.service.proto.Measurements.Quality


trait VariableSource {
  def forName(name: String): OperationValue
}

sealed trait OperationValue
case class NumericValue(value: Double) extends OperationValue
case class BooleanValue(value: Boolean) extends OperationValue
case class NumericMeas(value: Double, time: Option[Long]) extends OperationValue
case class BooleanMeas(value: Boolean, time: Option[Long]) extends OperationValue

trait Operation {
  def name: String
  def apply(inputs: List[OperationValue]): OperationValue
}

trait OperationSource {
  def forName(name: String): Operation
}

trait Expression {
  def evaluate(inputs: VariableSource, ops: OperationSource): OperationValue
}
