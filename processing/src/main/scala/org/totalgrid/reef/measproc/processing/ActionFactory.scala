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

import org.totalgrid.reef.client.service.proto.Measurements._
import org.totalgrid.reef.client.service.proto.Events._
import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.Processing.{ Action => ActionProto, ActivationType => TypeProto, LinearTransform => LinearProto, EventGeneration }
import org.totalgrid.reef.event._
import org.totalgrid.reef.persistence.ObjectCache

/**
 * Trigger/Action factory with constructor dependencies.
 */
class TriggerProcessingFactory(protected val publishEvent: Event.Builder => Unit, protected val lastCache: ObjectCache[Measurement])
  extends ProcessingResources
  with TriggerFactory
  with ActionFactory

/**
 * Internal interface for Trigger/Action factory dependencies
 */
trait ProcessingResources {
  protected val publishEvent: (Event.Builder) => Unit
  protected val lastCache: ObjectCache[Measurement]
}

/**
 * Factory for Action implementation objects
 */
trait ActionFactory { self: ProcessingResources =>
  import Actions._
  import TriggerFactory._

  /**
   * Creates Action objects given protobuf representation
   * @param proto   Action configuration proto
   * @return        Constructed action
   */
  def buildAction(proto: ActionProto): Action = {
    val name = proto.getActionName
    val typ = convertActivation(proto.getType)
    val disabled = proto.getDisabled

    if (proto.hasSuppress && proto.getSuppress) {
      new SuppressAction(name, disabled, typ)
    } else {
      val eval = if (proto.hasLinearTransform) {
        new LinearTransform(proto.getLinearTransform.getScale, proto.getLinearTransform.getOffset)
      } else if (proto.hasQualityAnnotation) {
        new AnnotateQuality(proto.getQualityAnnotation)
      } else if (proto.hasStripValue && proto.getStripValue) {
        new StripValue
      } else if (proto.hasSetBool) {
        new SetBool(proto.getSetBool)
      } else if (proto.hasSetUnit) {
        new SetUnit(proto.getSetUnit)
      } else if (proto.hasEvent) {
        new EventGenerator(publishEvent, proto.getEvent.getEventType)
      } else if (proto.hasBoolTransform) {
        new BoolEnumTransformer(proto.getBoolTransform.getFalseString, proto.getBoolTransform.getTrueString)
      } else if (proto.hasIntTransform) {
        import scala.collection.JavaConversions._
        val intMapping = proto.getIntTransform.getMappingsList.toList.map { vm => vm.getValue -> vm.getString }.toMap
        new IntegerEnumTransformer(intMapping)
      } else {
        throw new Exception("Must specify at least one action")
      }

      new BasicAction(name, disabled, typ, eval)
    }
  }
}

object Actions {

  class AnnotateQuality(qual: Quality) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      Measurement.newBuilder(m).setQuality(Quality.newBuilder(m.getQuality).mergeFrom(qual)).build
    }
  }
  class StripValue extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      Measurement.newBuilder(m)
        .clearDoubleVal
        .clearIntVal
        .clearStringVal
        .clearBoolVal
        .setType(Measurement.Type.NONE)
        .build
    }
  }
  class SetBool(b: Boolean) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      Measurement.newBuilder(m)
        .clearDoubleVal
        .clearIntVal
        .clearStringVal
        .setBoolVal(b)
        .setType(Measurement.Type.BOOL)
        .build
    }
  }
  class SetUnit(unit: String) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      Measurement.newBuilder(m).setUnit(unit).build
    }
  }
  class LinearTransform(scale: Double, offset: Double) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      m.getType match {
        case Measurement.Type.DOUBLE =>
          if (m.hasDoubleVal) Measurement.newBuilder(m).setDoubleVal(m.getDoubleVal * scale + offset).build else m
        case Measurement.Type.INT =>
          if (m.hasIntVal) Measurement.newBuilder(m).setIntVal((m.getIntVal * scale + offset).toLong).build else m
        case _ => m
      }
    }
  }

  class EventGenerator(out: Event.Builder => Unit, eventType: String)
      extends Action.Evaluation {

    def apply(m: Measurement): Measurement = {

      val alist = new AttributeList
      alist += ("validity" -> AttributeString(m.getQuality.getValidity.toString))
      if (m.getType.getNumber != Measurement.Type.NONE.getNumber) {
        m.getType match {
          case Measurement.Type.INT => alist += ("value" -> AttributeLong(m.getIntVal))
          case Measurement.Type.DOUBLE => alist += ("value" -> AttributeDouble(m.getDoubleVal))
          case Measurement.Type.BOOL => alist += ("value" -> AttributeBoolean(m.getBoolVal))
          case Measurement.Type.STRING => alist += ("value" -> AttributeString(m.getStringVal))
          case Measurement.Type.NONE =>
        }
        alist += ("unit" -> AttributeString(m.getUnit))
      }

      val builder = Event.newBuilder
        .setEventType(eventType)
        .setEntity(Entity.newBuilder.setName(m.getName))
        .setArgs(alist.toProto)
      if (m.getIsDeviceTime) builder.setDeviceTime(m.getTime)
      out(builder)
      m
    }
  }

  class BoolEnumTransformer(falseString: String, trueString: String) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      if (!m.hasBoolVal) {
        // TODO: handle non boolean measurements in enum transform
        m
      } else {
        m.toBuilder.setType(Measurement.Type.STRING)
          .setStringVal(if (m.getBoolVal) trueString else falseString).build
      }
    }
  }

  // integer as in "not floating point", not integer as in 2^32, values on measurements are actually Longs
  class IntegerEnumTransformer(mapping: Map[Long, String]) extends Action.Evaluation {
    def apply(m: Measurement): Measurement = {
      if (!m.hasIntVal) {
        // TODO: handle non int measurements in int transform
        m
      } else {
        mapping.get(m.getIntVal) match {
          case Some(s) => m.toBuilder.setStringVal(s).setType(Measurement.Type.STRING).build
          case None => m // TODO: how to handle unknown states in int transform?
        }

      }
    }
  }

}