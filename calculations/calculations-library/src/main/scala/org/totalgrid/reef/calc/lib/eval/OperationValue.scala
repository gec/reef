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

sealed trait OperationValue {
  def toList: List[OperationValue] = List(this)
}
object OperationValue {
  def combine(a1: OperationValue, a2: OperationValue): OperationValue = {
    (a1, a2) match {
      case (NumericValue(v1), NumericValue(v2)) => NumericConst(v1 + v2)
      case _ => throw new EvalException("Cannot combine " + a1 + " and " + a2)
    }
  }
}

case class ValueRange(list: List[OperationValue]) extends OperationValue {
  override def toList: List[OperationValue] = list
}

trait NumericValue extends OperationValue {
  def doubleValue: Double
}
object NumericValue {
  def unapply(v: NumericValue): Option[Double] = Some(v.doubleValue)
}
case class NumericConst(doubleValue: Double) extends NumericValue
case class NumericMeas(doubleValue: Double, time: Long) extends NumericValue

// we can have both classes defined but can only match on one at a time.
// weird bug where you can't match on both types in same match block
// https://issues.scala-lang.org/browse/SI-5081
// https://issues.scala-lang.org/browse/SI-4832
trait LongValue extends NumericValue {
  def longValue: Long
  def doubleValue = longValue.toDouble
}
object LongValue {
  def unapply(v: LongValue): Option[Long] = Some(v.longValue)
}

case class LongConst(longValue: Long) extends LongValue
case class LongMeas(longValue: Long, time: Long) extends LongValue

trait BooleanValue extends OperationValue {
  def value: Boolean
}
object BooleanValue {
  def unapply(v: BooleanValue): Option[Boolean] = Some(v.value)
}

case class BooleanConst(value: Boolean) extends BooleanValue
case class BooleanMeas(value: Boolean, time: Long) extends BooleanValue

