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
package org.totalgrid.reef.measproc.processing

import org.totalgrid.reef.api.proto.Measurements._
import org.totalgrid.reef.api.proto.Processing._
import org.totalgrid.reef.api.proto.Processing.{ Trigger => TriggerProto, AnalogLimit => AnalogLimitProto, ActivationType => TypeProto }
import collection.JavaConversions._
import org.totalgrid.reef.util.Optional._
import org.totalgrid.reef.api.proto.OptionalProtos._

object TriggerFactory {
  import Triggers._

  /**
   * Converts proto activation type to scala activation type
   * @param proto   Proto activation type
   * @return        Scala activation type
   */
  def convertActivation(proto: TypeProto) = proto match {
    case TypeProto.HIGH => Action.High
    case TypeProto.LOW => Action.Low
    case TypeProto.RISING => Action.Rising
    case TypeProto.FALLING => Action.Falling
    case TypeProto.TRANSITION => Action.Transition
  }

  /**
   * Constructs either an upper,lower or, most commonly, range condition
   * @param proto   Proto configuration
   * @return        Trigger condition
   */
  def limitCondition(proto: AnalogLimitProto): Trigger.Condition = {
    // TODO: do we need non-deadbanded limit checks, does deadband == 0 work for all floats
    if (proto.hasLowerLimit && proto.hasUpperLimit) {
      if (proto.hasDeadband)
        new RangeLimitDeadband(proto.getUpperLimit, proto.getLowerLimit, proto.getDeadband)
      else
        new RangeLimit(proto.getUpperLimit, proto.getLowerLimit)
    } else if (proto.hasUpperLimit()) {
      if (proto.hasDeadband)
        new UpperLimitDeadband(proto.getUpperLimit, proto.getDeadband)
      else
        new UpperLimit(proto.getUpperLimit)
    } else if (proto.hasLowerLimit()) {
      if (proto.hasDeadband)
        new LowerLimitDeadband(proto.getLowerLimit, proto.getDeadband)
      else
        new LowerLimit(proto.getLowerLimit)
    } else throw new IllegalArgumentException("Upper and Lower limit not set in AnalogLimit")
  }
}

/**
 * Factory for trigger implementation objects.
 */
trait TriggerFactory { self: ActionFactory =>
  import TriggerFactory._
  import Triggers._

  /**
   * Creates trigger objects given protobuf configuration.
   * @param proto         Configuration object
   * @param pointName     Associated measurement point
   * @return              Implementation object
   */
  def buildTrigger(proto: TriggerProto, pointName: String): Trigger = {
    val cacheID = pointName + "." + proto.getTriggerName
    val stopProc = proto.hasStopProcessingWhen thenGet convertActivation(proto.getStopProcessingWhen)
    val conditions = List(
      proto.analogLimit.map(limitCondition(_)),
      proto.quality.map(new QualityCondition(_)),
      proto.unit.map(new UnitCondition(_)),
      proto.valueType.map(new TypeCondition(_)),
      proto.boolValue.map(new BoolValue(_)),
      proto.intValue.map(new IntegerValue(_)),
      proto.stringValue.map(new StringValue(_))).flatten

    val actions = proto.getActionsList.toList.map(buildAction(_))
    new BasicTrigger(cacheID, conditions, actions, stopProc)
  }
}

/**
 * Implementations of corresponding proto Trigger types (see Triggers.proto)
 */
object Triggers {
  class BoolValue(b: Boolean) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      (m.getType == Measurement.Type.BOOL) &&
        (m.getBoolVal == b)
    }
  }

  // integer as in "not floating point", not integer as in 2^32, values on measurements are actually Longs
  class IntegerValue(i: Long) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      (m.getType == Measurement.Type.INT) &&
        (m.getIntVal == i)
    }
  }

  class StringValue(s: String) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      (m.getType == Measurement.Type.STRING) &&
        (m.getStringVal == s)
    }
  }

  class RangeLimit(upper: Double, lower: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      x <= lower || x >= upper
    }
  }

  class RangeLimitDeadband(upper: Double, lower: Double, deadband: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      (prev && (x <= (lower + deadband) || x >= (upper - deadband))) || (!prev && (x <= lower || x >= upper))
    }
  }

  class UpperLimitDeadband(limit: Double, deadband: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      (prev && x >= (limit - deadband)) || (!prev && x >= limit)
    }
  }
  class UpperLimit(limit: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      x >= limit
    }
  }

  class LowerLimitDeadband(limit: Double, deadband: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      (prev && x <= (limit + deadband)) || (!prev && x <= limit)
    }
  }
  class LowerLimit(limit: Double) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      val x = Trigger.analogValue(m) getOrElse { return false }
      x <= limit
    }
  }

  class QualityCondition(qual: Quality) extends Trigger.Condition {
    import org.totalgrid.reef.api.proto.OptionalProtos._

    def apply(m: Measurement, prev: Boolean): Boolean = {
      val q = m.getQuality
      qual.validity == q.validity ||
        qual.source == q.source ||
        qual.test == q.test ||
        qual.operatorBlocked == q.operatorBlocked ||
        qual.detailQual.overflow == q.detailQual.overflow ||
        qual.detailQual.outOfRange == q.detailQual.outOfRange ||
        qual.detailQual.badReference == q.detailQual.badReference ||
        qual.detailQual.oscillatory == q.detailQual.oscillatory ||
        qual.detailQual.failure == q.detailQual.failure ||
        qual.detailQual.oldData == q.detailQual.oldData ||
        qual.detailQual.inconsistent == q.detailQual.inconsistent ||
        qual.detailQual.inaccurate == q.detailQual.inaccurate
    }
  }

  class UnitCondition(unit: String) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      (m.hasUnit && m.getUnit == unit)
    }
  }

  class TypeCondition(typ: Measurement.Type) extends Trigger.Condition {
    def apply(m: Measurement, prev: Boolean): Boolean = {
      (m.getType == typ)
    }
  }
}

