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
package org.totalgrid.reef.loader

import equipment._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Model.{ Point, Command, Entity }
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.communications._
import org.totalgrid.reef.proto.Model.{ PointType => PointTypeProto, ConfigFile => ConfigFileProto }
import java.io.File
import org.totalgrid.reef.loader.common.ConfigFile
import com.google.protobuf.ByteString

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

    val proto = TriggerSet.newBuilder
      .setPoint(toPoint(pointName))
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

  def addTriggers(client: ModelLoader, point: Point, triggers: List[Trigger.Builder]) {
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
   * we need to decide on the order of operations for the triggers. In most circumstances you would not have
   * more than one trigger type of each class but we give each a different priority so if they do try the
   * result will be deterministic. Lowest priorities are done first. Most of the triggers provide a way to
   * override the default priority if the user has to.
   */
  object DefaultPriorities {
    /* pre-conversion triggers should be between 0-100 */
    val RLC_RANGE = 50 /// RLC range is defined as a range check but with unit == raw
    /* transformation triggers should be between 100-200 */
    val SCALING = 150
    val ENUM_TRANSFORM = 151
    /* transformation triggers should be between 400-500 */
    val UNEXPECTED = 450
    val REGULAR_RANGE = 451
  }

  /**
   * Low, High, Deadband
   * Low, Deadband
   * High, Deadband
   * Deadband is always optional.
   */
  def toTrigger(pointName: String, range: Range, unit: String, actionModel: HashMap[String, ActionSet]): Trigger.Builder = {
    val name = pointName + "." + range.getActionSet
    val proto = Trigger.newBuilder.setTriggerName(name)

    val fromUnit = if (range.isSetUnit) range.getUnit else unit
    proto.setUnit(fromUnit)

    val al = AnalogLimit.newBuilder()
    if (range.isSetLow)
      al.setLowerLimit(range.getLow)
    if (range.isSetHigh)
      al.setUpperLimit(range.getHigh)
    al.setDeadband(range.getDeadband)

    proto.setAnalogLimit(al)

    val defaultPriority = if (fromUnit == "raw") DefaultPriorities.RLC_RANGE else DefaultPriorities.REGULAR_RANGE

    val actionSet = getActionSet(actionModel, name, range)
    addActions(name, proto, actionSet, defaultPriority)

    proto
  }

  def toTrigger(pointName: String, enumTransform: Transform, unit: String): Trigger.Builder = {
    val name = pointName + ".convert"
    val proto = Trigger.newBuilder.setTriggerName(name)

    val fromUnit = if (enumTransform.isSetFromUnit) enumTransform.getFromUnit else "raw"
    proto.setUnit(fromUnit)

    val priority = if (enumTransform.isSetPriority) enumTransform.getPriority else DefaultPriorities.ENUM_TRANSFORM
    proto.setPriority(priority)

    if (enumTransform.isSetToUnit && enumTransform.getToUnit != enumTransform.getFromUnit) {
      proto.addActions(toActionSetUnit(name, ActivationType.HIGH, enumTransform.getToUnit))
    }

    val actionBuilder = Action.newBuilder.setActionName(name).setType(ActivationType.HIGH)

    val conversions = enumTransform.getValueMap.toList

    if (!enumTransform.isSetTransformationType) {
      throw new Exception("Transformation for point: '" + pointName + "' doesn't have legal type setting, should be \"status\" or \"counter\".")
    }
    enumTransform.getTransformationType match {
      case TransformType.STATUS => createBooleanMapping(conversions, actionBuilder, pointName)
      case TransformType.COUNTER => createIntegerMapping(conversions, actionBuilder, pointName)
    }

    proto.addActions(actionBuilder)

    proto
  }

  private def createBooleanMapping(conversions: List[ValueMap], actionBuilder: Action.Builder, pointName: String) {
    try {
      conversions.foreach(vm => vm.getFromValue.toBoolean)
      val convertMap = conversions.map { vm => vm.getFromValue.toBoolean -> vm.getToValue }.toMap
      if (convertMap.size != 2) {
        throw new Exception("Transformation for status point: '" + pointName + "' doesn't define output values for both true and false.")
      }
      actionBuilder.setBoolTransform(BoolEnumTransform.newBuilder.setFalseString(convertMap(false)).setTrueString(convertMap(true)))
    } catch {
      case _: NumberFormatException =>
        throw new Exception("Not all \"fromValues\" for transformation on point: '" + pointName + "' are convertible to booleans. Use \"true\" or \"false\".")
    }
  }

  private def createIntegerMapping(conversions: List[ValueMap], actionBuilder: Action.Builder, pointName: String) {
    try {
      conversions.foreach(vm => vm.getFromValue.toInt)
      val intTransform = IntEnumTransform.newBuilder()
      conversions.foreach(vm => intTransform.addMappings(IntToString.newBuilder.setValue(vm.getFromValue.toInt).setString(vm.getToValue)))
      actionBuilder.setIntTransform(intTransform)
    } catch {
      case _: NumberFormatException =>
        throw new Exception("Not all \"fromValues\" for transformation on point: '" + pointName + "' are convertible to integers. Use only whole numbers.")
    }
  }

  def toTrigger(pointName: String, unexpected: Unexpected, unit: String, actionModel: HashMap[String, ActionSet]): Trigger.Builder = {
    val name = pointName + "." + unexpected.getActionSet
    val proto = Trigger.newBuilder.setTriggerName(name)

    if (unexpected.isSetBooleanValue)
      proto.setBoolValue(unexpected.isBooleanValue)
    if (unexpected.isSetStringValue)
      proto.setStringValue(unexpected.getStringValue)
    if (unexpected.isSetIntValue)
      proto.setIntValue(unexpected.getIntValue)

    val actionSet = getActionSet(actionModel, name, unexpected)
    addActions(name, proto, actionSet, DefaultPriorities.UNEXPECTED)

    proto
  }

  /**
   * Add an action for each trigger type.
   */
  def processTriggerType(trigger: Trigger.Builder, name: String, actions: TriggerType, aType: ActivationType): Unit = {

    if (!actions.isMoreActions)
      trigger.setStopProcessingWhen(aType)

    if (actions.isSetMessage)
      trigger.addActions(toActionMessage(name, aType, actions.getMessage))

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

  def addActions(name: String, proto: Trigger.Builder, actionSet: ActionSet, defaultPriority: Int) {

    val priority = if (actionSet.isSetPriority) actionSet.getPriority else defaultPriority

    proto.setPriority(priority)

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
      .setPriority(DefaultPriorities.SCALING)
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
    toActionSetUnit(name, aType, setUnit.getUnit) // TODO: check if attribute is there.
  }
  def toActionSetUnit(name: String, aType: ActivationType, unit: String): Action.Builder = {
    Action.newBuilder
      .setActionName(name + ".SetUnit")
      .setType(aType)
      .setSetUnit(unit)
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

  def validatePointScale(ex: ExceptionCollector, parent: String, scale: Scale): Unit = {
    ex.collect("PointScaling: " + parent) {
      if (!(scale.isSetRawLow && scale.isSetRawHigh && scale.isSetEngLow && scale.isSetEngHigh) &&
        !(scale.isSetSlope && scale.isSetOffset)) {
        throw new Exception("<scale> element in " + parent + " does not have require attributes: (rawLow,rawHigh,engLow,engHigh) or (slope,offset)")
      }

      if (!scale.isSetEngUnit)
        throw new Exception("<scale> element in " + parent + " does not have require attribute engUnit")
    }
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

  def toPoint(name: String): Point = Point.newBuilder.setName(name).build

  def toPoint(name: String, entity: Entity, pointType: PointTypeProto, unit: String): Point = {
    val proto = Point.newBuilder
      .setName(name)
      .setEntity(entity)
      .setType(pointType)
      .setUnit(unit)

    proto.build
  }

  def toConfigFile(cf: ConfigFile, rootDir: File): ConfigFileProto = {

    if (!cf.isSetName && !cf.isSetFileName) throw new LoadingException("Need to set either fileName or name for configFile.")

    val name = if (cf.isSetName) cf.getName else cf.getFileName

    val hasCData = cf.isSetValue && cf.getValue.size > 0
    val hasFilename = cf.isSetFileName

    if (hasFilename && hasCData) throw new LoadingException("Cannot have both filename and inline-data for configFile: " + name)

    if (!hasFilename && !hasCData) throw new LoadingException("ConfigFile must specify either fileName or include inline-data: " + name)

    val mimeType = if (!cf.isSetMimeType) {

      name match {
        case s: String if (s.endsWith(".xml")) => "text/xml"
        case _ => throw new LoadingException("Cannot guess mimeType for configfile, must be explictly included: " + name)
      }

    } else {
      cf.getMimeType
    }

    val proto = ConfigFileProto.newBuilder
      .setName(name)
      .setMimeType(mimeType)

    val bytes = if (hasFilename) {
      val file = new File(rootDir, cf.getFileName)
      if (!file.exists()) throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " doesn't exist.")
      scala.io.Source.fromFile(file).mkString.getBytes
    } else {
      cf.getValue.getBytes
    }
    proto.setFile(ByteString.copyFrom(bytes))
    proto.build
  }
}
