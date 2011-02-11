/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.loader

import equipment.{ Unexpected, Range }
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Model.{ Point, Command, Entity }
import org.totalgrid.reef.loader.communications.Scale
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.communications._
import org.totalgrid.reef.protoapi.scala.client.SyncOperations

/**
 * Utility methods to crate protos
 */
object ProtoUtils {

  def toTriggerSet(point: Point): TriggerSet = {
    val proto = TriggerSet.newBuilder
      .setPoint(point)
    proto.build
  }

  def toTriggerSet(point: Point, trigger: Trigger.Builder): TriggerSet = {
    val proto = TriggerSet.newBuilder
      .setPoint(point)
      .addTriggers(trigger)
    proto.build
  }

  def toTriggerSet(pointName: String, trigger: Trigger.Builder): TriggerSet = {

    val pointEntity = toEntityType(pointName, List("Point"))
    val point = toPoint(pointName, pointEntity)

    val proto = TriggerSet.newBuilder
      .setPoint(point)
      .addTriggers(trigger)
    proto.build
  }

  /**
   * Insert range triggers to the existing trigger set in the system.
   * Overwrite any triggers with the same name.
   * Do not clear the current TriggerSet.
   *
   * RANGE:
   * Low, High, Deadband
   * Low, Deadband
   * High, Deadband
   * Deadband is always optional.
   */
  def insertTrigger(triggerSet: TriggerSet.Builder, trigger: Trigger.Builder): TriggerSet.Builder = {
    //println("insertTrigger: trigger: " + trigger.toString)
    //println("insertTrigger: before insert triggerSet: \n" + triggerSet.toString)

    //  Get the current list of triggers minus the trigger we're inserting (if it existed in the triggerSet).
    var triggers = triggerSet.getTriggersList.toList.filter(_.getTriggerName != trigger.getTriggerName).map(_.toBuilder)
    triggers ::= trigger
    triggers = triggers.sortBy(_.getPriority)

    triggerSet.clearTriggers
    triggers.foreach(triggerSet.addTriggers(_))

    //println("insertTrigger: after insert triggerSet: \n" + triggerSet.toString)

    triggerSet
  }

  def addTriggers(client: SyncOperations, point: Point, triggers: List[Trigger.Builder]) {
    var triggerSets = client.getOrThrow(toTriggerSet(point))
    var triggerSet = triggerSets.size match {
      case 0 => TriggerSet.newBuilder.setPoint(point)
      case 1 => triggerSets.head.toBuilder
      case n => throw new Exception("Service returned multiple TriggerSets for point '" + point.getName + "'. Should be one or none.")
    }
    triggers.foreach(trigger => triggerSet = insertTrigger(triggerSet, trigger))
    val ts = triggerSet.build
    client.putOrThrow(ts)
  }

  /**
   * Low, High, Deadband
   * Low, Deadband
   * High, Deadband
   * Deadband is always optional.
   */
  def toTrigger(pointName: String, range: Range, unit: String, actionModel: HashMap[String, ActionSet]): Trigger.Builder = {
    val name = pointName + "." + range.getActionSet
    val proto = Trigger.newBuilder
      .setTriggerName(name)
      .setUnit(unit)

    if (range.getActionSet == "RLC") // TODO: This should be in the communications model and setup correctly.
      proto.setUnit("raw")

    val al = AnalogLimit.newBuilder()
    if (range.isSetLow)
      al.setLowerLimit(range.getLow)
    if (range.isSetHigh)
      al.setUpperLimit(range.getHigh)
    al.setDeadband(range.getDeadband)

    proto.setAnalogLimit(al)

    val actionSet = getActionSet(actionModel, name, range)
    addActions(name, proto, actionSet)

    proto
  }

  def toTrigger(pointName: String, unexpected: Unexpected, unit: String, actionModel: HashMap[String, ActionSet]): Trigger.Builder = {
    val name = pointName + "." + unexpected.getActionSet
    val proto = Trigger.newBuilder
      .setTriggerName(name)
    //  .setUnit(unit)

    // TODO: implement unexpected strings and ints
    if (unexpected.isSetBooleanValue)
      proto.setBoolValue(unexpected.isBooleanValue)

    val actionSet = getActionSet(actionModel, name, unexpected)
    addActions(name, proto, actionSet)

    proto
  }

  /**
   * Add an action for each trigger type.
   */
  def processTriggerType(trigger: Trigger.Builder, name: String, actions: TriggerType, aType: ActivationType): Unit = {

    if (!actions.isMoreActions)
      trigger.setStopProcessingWhen(aType)

    actions.getMessage.toList.foreach(m => trigger.addActions(toActionMessage(name, aType, m)))

    if (actions.isSetStripValue)
      trigger.addActions(toActionStripValue(name, aType))

    if (actions.isSetSetBool)
      trigger.addActions(toActionSetBool(name, aType, actions.getSetBool))

    if (actions.isSetSetUnit)
      trigger.addActions(toActionSetUnit(name, aType, actions.getSetUnit))

    if (actions.isSetSetAbnormal)
      trigger.addActions(toActionSetAbnormal(name, aType))

  }

  def getActionSet(actionModel: HashMap[String, ActionSet], elementName: String, range: Range): ActionSet = {
    if (!range.isSetActionSet)
      throw new Exception("<range> element used by point '" + elementName + "' does not actionSet attribute.")
    val asName = range.getActionSet
    if (actionModel.contains(asName))
      actionModel(asName)
    else
      throw new Exception("range actionSet=\"" + asName + "\"  referenced from '" + elementName + "' was not found in configuration.")
  }

  def getActionSet(actionModel: HashMap[String, ActionSet], elementName: String, unexpected: Unexpected): ActionSet = {
    if (!unexpected.isSetActionSet)
      throw new Exception("<unexpected> element used by point '" + elementName + "' does not actionSet attribute.")
    val asName = unexpected.getActionSet
    if (actionModel.contains(asName))
      actionModel(asName)
    else
      throw new Exception("unexpected actionSet=\"" + asName + "\"  referenced from '" + elementName + "' was not found in configuration.")
  }

  def addActions(name: String, proto: Trigger.Builder, actionSet: ActionSet) {
    proto.setPriority(actionSet.getPriority)

    if (actionSet.isSetRising)
      processTriggerType(proto, name, actionSet.getRising, ActivationType.RISING)
    if (actionSet.isSetFalling)
      processTriggerType(proto, name, actionSet.getFalling, ActivationType.FALLING)
    if (actionSet.isSetTransition)
      processTriggerType(proto, name, actionSet.getTransition, ActivationType.TRANSITION)
    // give priority to edge actions TODO: should allow arbitrary order of actions
    if (actionSet.isSetHigh)
      processTriggerType(proto, name, actionSet.getHigh, ActivationType.HIGH)
    if (actionSet.isSetLow)
      processTriggerType(proto, name, actionSet.getLow, ActivationType.LOW)
  }

  /**
   * Communications model point scaling.
   */
  def toTrigger(name: String, scale: Scale): Trigger.Builder = {
    val proto = Trigger.newBuilder
      .setTriggerName(name + ".scale")
      .setPriority(250) // TODO: get from config!
      .setUnit(scale.getRawUnit)

    proto.addActions(toActionLinearTransform(name, scale))

    //  Set the "from" unit.
    // We've already set engUnit (the "to" unit) in Action.
    // The communications.xsd defaults this to "raw"
    proto.addActions(toActionSetUnit(name, ActivationType.HIGH, scale.getEngUnit))

    proto
  }

  def toActionMessage(name: String, aType: ActivationType, message: Message): Action.Builder = {
    Action.newBuilder
      .setActionName(name + "." + message.getName)
      .setType(aType)
      .setEvent(EventGeneration.newBuilder.setEventType(message.getName))
  }

  def toActionStripValue(name: String, aType: ActivationType): Action.Builder = {
    Action.newBuilder
      .setActionName(name + ".StripValue")
      .setType(aType)
      .setStripValue(true)
  }

  def toActionSetBool(name: String, aType: ActivationType, setBool: SetBool): Action.Builder = {
    Action.newBuilder
      .setActionName(name + ".SetBool")
      .setType(aType)
      .setSetBool(setBool.isValue) // TODO: check if attribute is there.
  }

  def toActionSetUnit(name: String, aType: ActivationType, setUnit: SetUnit): Action.Builder = {
    toActionSetUnit(name, aType, setUnit.getUnit)
  }
  def toActionSetUnit(name: String, aType: ActivationType, unit: String): Action.Builder = {
    Action.newBuilder
      .setActionName(name + ".SetUnit")
      .setType(aType)
      .setSetUnit(unit) // TODO: check if attribute is there.
  }

  def toActionSetAbnormal(name: String, aType: ActivationType): Action.Builder = {
    import org.totalgrid.reef.proto.Measurements._
    val q = Quality.newBuilder()
      .setValidity(Quality.Validity.QUESTIONABLE)
      .setDetailQual(DetailQual.newBuilder.setInconsistent(true))

    Action.newBuilder
      .setActionName(name + ".SetAbnormal")
      .setType(aType)
      .setQualityAnnotation(q)
  }

  def toActionLinearTransform(name: String, scale: Scale): Action.Builder = {

    val ltProto = LinearTransform.newBuilder

    if (scale.isSetRawLow && scale.isSetRawHigh && scale.isSetEngLow && scale.isSetEngHigh) {
      val rawRange = scale.getRawHigh - scale.getRawLow
      val engRange = scale.getEngHigh - scale.getEngLow
      val scaleValue = engRange / rawRange
      ltProto.setScale(scaleValue)
      ltProto.setOffset(scale.getEngLow - scale.getRawLow * scaleValue)
    } else if (scale.isSetSlope && scale.isSetOffset) {
      ltProto.setScale(scale.getSlope)
      ltProto.setOffset(scale.getOffset)
    } else {
      throw new Exception("<scale> element used by point '" + name + "' does not have attributes: (rawLow,rawHigh,engLow,engHigh) or (slope,offset)")
    }

    if (!scale.isSetEngUnit)
      throw new Exception("<scale> element used by point '" + name + "' does not have attribute engUnit")

    val proto = Action.newBuilder
      .setActionName(name + ".scale")
      .setType(ActivationType.HIGH)
      .setLinearTransform(ltProto)

    proto
  }

  def validatePointScale(parent: String, scale: Scale): Unit = {
    if (!(scale.isSetRawLow && scale.isSetRawHigh && scale.isSetEngLow && scale.isSetEngHigh) &&
      !(scale.isSetSlope && scale.isSetOffset)) {
      throw new Exception("<scale> element in " + parent + " does not have require attributes: (rawLow,rawHigh,engLow,engHigh) or (slope,offset)")
    }

    if (!scale.isSetEngUnit)
      throw new Exception("<scale> element in " + parent + " does not have require attribute engUnit")
  }

  /**
   * Return and Entity proto
   *
   * @param types  List of type strings
   */
  def toEntityType(name: String, types: List[String]): Entity = {
    val proto = Entity.newBuilder
      .setName(name)
    types.foreach(proto.addTypes)
    proto.build
  }

  def toPoint(name: String, entity: Entity): Point = {
    val proto = Point.newBuilder
      .setName(name)
      .setEntity(entity)

    proto.build
  }

}
