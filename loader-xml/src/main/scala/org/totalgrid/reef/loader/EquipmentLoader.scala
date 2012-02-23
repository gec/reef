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

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.client.service.proto.Processing.{ Filter => FilterProto, _ }
import org.totalgrid.reef.client.service.proto.Model.{ Entity, EntityEdge, Command => CommandProto }
import org.totalgrid.reef.client.service.proto.Model.{ PointType => PointTypeProto, CommandType => CommandTypeProto }
import org.totalgrid.reef.loader.equipment._

import org.totalgrid.reef.loader.EnhancedXmlClasses._
import org.totalgrid.reef.loader.calculations.Calculation

/**
 * EquipmentLoader loads the logical model.
 *
 */
class EquipmentLoader(modelLoader: ModelLoader, loadCache: LoadCacheEquipment, exceptionCollector: ExceptionCollector, commonLoader: CommonLoader)
    extends Logging with BaseConfigurationLoader {

  val equipmentProfiles = LoaderMap[EquipmentType]("Equipment Profile")
  val pointProfiles = LoaderMap[PointProfile]("Point Profile")
  val commands = LoaderMap[CommandProto]("Command")
  val commandEntities = LoaderMap[Entity]("Command Entities")

  val equipments = LoaderMap[Entity]("Equipment")

  // map of points to units
  val equipmentPointUnits = LoaderMap[String]("Point Unit")

  /**
   * Reset all class variables
   */
  def reset: Unit = {
    equipmentProfiles.clear
    pointProfiles.clear
    commands.clear
    commandEntities.clear
    equipmentPointUnits.clear
    equipments.clear
    modelLoader.reset()
  }

  def getExceptionCollector: ExceptionCollector = {
    exceptionCollector
  }

  def getModelLoader: ModelLoader = {
    modelLoader
  }

  /**
   * Load this equipment node and all children. Create edges to connect the children.
   * Return equipmentPointUnits - map of points to units
   */
  def load(model: EquipmentModel, actionModel: HashMap[String, ActionSet]): HashMap[String, String] = {
    logger.info("Equipment Loader Started")

    exceptionCollector.collect("Equipment Profiles: ") {
      // Collect all the profiles in name->profile maps.
      Option(model.getProfiles).foreach { profile =>
        profile.getPointProfile.toList.foreach(pointProfile => pointProfiles += (pointProfile.getName -> pointProfile))
        profile.getEquipmentProfile.toList.foreach(equipmentProfile => equipmentProfiles += (equipmentProfile.getName -> equipmentProfile))
      }
    }

    println("Loading Equipment: Found PointProfiles: " + pointProfiles.keySet.mkString(", "))
    println("Loading Equipment: Found EquipmentProfiles: " + equipmentProfiles.keySet.mkString(", "))

    model.getEquipment.toList.foreach(equipment => {
      println("Loading Equipment: processing equipment '" + equipment.getName + "'")
      exceptionCollector.collect("Equipment: " + equipment.getName) {
        loadEquipment(equipment, None, actionModel)
      }
    })

    logger.info("Equipment Loader Ended")

    equipmentPointUnits
  }

  /**
   * Load this equipment node and all children. Create edges to connect the children.
   */
  private def loadEquipment(equipment: Equipment, namePrefix: Option[String], actionModel: HashMap[String, ActionSet]): Entity = {
    val name = getChildName(namePrefix, equipment.getName)
    val childPrefix = if (equipment.isAddParentNames) Some(name + ".") else None

    // IMPORTANT: profiles is a list of profiles plus this equipment (as the last "profile" in the list)
    // TODO: We don't see profiles within profiles. Could go recursive and map each profile name to a list of profiles.

    val profiles: List[EquipmentType] = equipment.getEquipmentProfile.toList.map(p => equipmentProfiles(p.getName)) ::: List[EquipmentType](equipment)
    // don't print last profile which is this equipment
    logger.info("load equipment '" + name + "' with profiles: " + profiles.map(_.getName).dropRight(1).mkString(", "))

    val childEquipment = profiles.flatMap(_.getEquipment)

    // recursive call to process children
    // Load all the children and create the edges
    logger.trace("load equipment: " + name + " children")
    val children = childEquipment.map(loadEquipment(_, childPrefix, actionModel))

    val controls = profiles.flatMap(_.getControl.toList)
    val setpoints = profiles.flatMap(_.getSetpoint.toList)
    val statuses = profiles.flatMap(_.getStatus.toList)
    val analogs = profiles.flatMap(_.getAnalog.toList)
    val counters = profiles.flatMap(_.getCounter.toList)

    val types = profiles.flatMap(_.getType.toList).map { _.getName }.distinct

    val points = statuses ::: analogs ::: counters
    val commands = controls ::: setpoints

    var extraTypes: List[String] = Nil
    if (points.nonEmpty || commands.nonEmpty) extraTypes ::= "Equipment"
    if (childEquipment.nonEmpty && types.find(s => s == "Site" || s == "Root").isEmpty) extraTypes ::= "EquipmentGroup"

    val foundEquipment = equipments.get(name)
    if (foundEquipment.isDefined) {
      if (points.nonEmpty || commands.nonEmpty || childEquipment.nonEmpty)
        throw new LoadingException("Reference to equipment cannot contain data")
      return foundEquipment.get
    }

    val entity = toEntity(name, types, extraTypes)
    modelLoader.putOrThrow(entity)

    equipment.getInfo.foreach(infoBlock => commonLoader.addInfo(entity, infoBlock))

    // Commands are controls and setpoints
    logger.trace("load equipment: " + name + " commands")
    controls.map { c =>
      processCommand(childPrefix, c, entity, CommandTypeProto.CONTROL)
    }

    setpoints.map { c =>
      val commandType = convertSetpointTypeToCommandType(c.getSetpointType)
      processCommand(childPrefix, c, entity, commandType)
    }

    // Points
    logger.trace("load equipment: " + name + " points")
    points.map(processPointType(_, entity, childPrefix, actionModel))

    // add the edge connecting us to our children
    children.foreach(child => modelLoader.putOrThrow(ProtoUtils.toEntityEdge(entity, child, "owns")))

    equipments += (name -> entity)

    entity
  }

  /**
   * Process controls defined under equipment.
   */
  def processCommand(childPrefix: Option[String], xmlCommand: Command, equipmentEntity: Entity, commandType: CommandTypeProto) = {
    import ProtoUtils._

    val name = getChildName(childPrefix, xmlCommand.getName)
    val displayName = Option(xmlCommand.getDisplayName) getOrElse xmlCommand.getName

    val types = "Command" :: getTypeList(xmlCommand.getType)

    logger.trace("processControl: " + name)
    loadCache.addControl(name)
    val commandEntity = toEntityType(name, types)
    val commandProto = toCommand(name, displayName, commandEntity, commandType)
    commandEntities += (name -> commandEntity)
    commands += (name -> commandProto)

    modelLoader.putOrThrow(commandEntity)

    xmlCommand.getInfo.foreach(i => commonLoader.addInfo(commandEntity, i))

    modelLoader.putOrThrow(commandProto)
    modelLoader.putOrThrow(ProtoUtils.toEntityEdge(equipmentEntity, commandEntity, "owns"))

    commandEntity
  }

  def getTypeList(list: java.util.List[Type]): List[String] = Option(list) match {
    //import scala.collection.JavaConversions._
    case Some(x) => x.map(_.getName).toList
    case None => Nil
  }

  /**
   * Put the point and pointEntity and setup the owns relationship.
   * Return the pointEntity
   *
   * TODO: Handle exception from invalid name references (ex: control)
   */
  def processPointType(pointType: PointType, equipmentEntity: Entity, childPrefix: Option[String], actionModel: HashMap[String, ActionSet]): Entity = {
    import ProtoUtils._

    val pointProtoType = pointType match {
      case c: equipment.Analog => PointTypeProto.ANALOG
      case c: equipment.Status => PointTypeProto.STATUS
      case c: equipment.Counter => PointTypeProto.COUNTER
      case _ => throw new LoadingException("Bad point type")
    }

    val name = getChildName(childPrefix, pointType.getName)
    val types = "Point" :: getTypeList(pointType.getType)

    val unit = getAttribute[String](name, pointType, _.isSetUnit, _.getUnit, "unit")
    equipmentPointUnits += (name -> unit)

    logger.trace("processPointType: " + name)
    val pointEntity = toEntityType(name, types)
    val point = toPoint(name, pointEntity, pointProtoType, unit)
    modelLoader.putOrThrow(pointEntity)

    pointType.getInfo.foreach(i => commonLoader.addInfo(pointEntity, i))

    modelLoader.putOrThrow(point)
    modelLoader.putOrThrow(ProtoUtils.toEntityEdge(equipmentEntity, pointEntity, "owns"))

    val controls = getElements[Control](name, pointType, _.getControl.toList)
    controls.map(c => modelLoader.putOrThrow(ProtoUtils.toEntityEdge(pointEntity, getCommandEntity(name, getChildName(childPrefix, c.getName)), "feedback")))

    val setpoint = getElements[Setpoint](name, pointType, _.getSetpoint.toList)
    setpoint.map(c => modelLoader.putOrThrow(ProtoUtils.toEntityEdge(pointEntity, getCommandEntity(name, getChildName(childPrefix, c.getName)), "feedback")))

    loadCache.addPoint(name, unit)
    // Insert range triggers to the existing trigger set in the system. Overwrite any triggers with the same name.
    // Do not clear the current TriggerSet.
    //

    var triggers = List.empty[Trigger.Builder]

    val ranges = getElements[Range](name, pointType, _.getRange.toList)
    triggers = triggers ::: ranges.map { range => toTrigger(name, range, unit, actionModel) }

    val unexpectedValues = getElements[Unexpected](name, pointType, _.getUnexpected.toList)
    triggers = triggers ::: unexpectedValues.map { unexpected => toTrigger(name, unexpected, unit, actionModel) }

    val convertValues = getElements[Transform](name, pointType, _.getTransform.toList)
    triggers = triggers ::: convertValues.map { transform => toTrigger(name, transform, unit) }

    val filterValues = getElements[Filter](name, pointType, _.getFilter.toList)
    if (filterValues.isEmpty) triggers :::= List(filterDefault(name))
    else triggers :::= filterValues.flatMap { filter => toTrigger(name, filter, pointProtoType) }

    if (!triggers.isEmpty) addTriggers(commonLoader.triggerCache, point, triggers)

    val calculations = getElements[Calculation](name, pointType, _.getCalculation.toList)
    calculations.foreach { c => addCalculation(name, childPrefix, c) }

    pointEntity
  }

  def addCalculation(pointName: String, childPrefix: Option[String], calc: Calculation) {
    exceptionCollector.collect("Calculation for point: " + pointName) {
      val (calcProto, sourcePoints) = CalculationsLoader.prepareCalculationProto(pointName, childPrefix.getOrElse(""), calc)
      // modelLoader.putOrThrow(calcProto)
      val edges = sourcePoints.map { sourceName =>
        equipmentPointUnits.get(sourceName).getOrElse(throw new LoadingException("Input point unknown: " + sourceName))
        ProtoUtils.toEntityEdge(sourceName, pointName, "calcs")
      }
      edges.foreach { modelLoader.putOrThrow(_) }
    }
  }

  /**
   * Get a point attribute in the point or the referenced pointProfile.
   * For this function, profiles can be nested and it will get the first on it
   * finds.
   * Throw exception if no attribute found.
   */
  def getAttribute[A](
    name: String,
    point: PointType,
    isSet: (PointType) => Boolean,
    get: (PointType) => A,
    attributeName: String): A = {

    val value: A = isSet(point) match {
      case true => get(point)
      case false =>
        point.isSetPointProfile match {
          case true => getAttribute[A](name, getPointProfile(name, point), isSet, get, attributeName)
          case false => throw new LoadingException("Point '" + name + "' is missing required attribute '" + attributeName + "'.")
        }
    }
    value
  }

  /**
   * get contained elements from this point and its pointProfile
   */
  def getElements[A](name: String, point: PointType, get: PointType => List[A]): List[A] = {

    if (point.isSetPointProfile)
      get(point) ::: getElements[A](name, getPointProfile(name, point), get)
    else
      get(point)
  }

  def getCommandEntity(elementName: String, commandName: String): Entity = {
    if (commandEntities.contains(commandName))
      commandEntities(commandName)
    else
      throw new LoadingException("control '" + commandName + "' referenced from '" + elementName + "' was not found in configuration.")
  }

  def getPointProfile(elementName: String, point: PointType): PointProfile = {
    val p = point.getPointProfile
    if (pointProfiles.contains(p))
      pointProfiles(p)
    else
      throw new LoadingException("pointProfile '" + p + "' referenced from '" + elementName + "' was not found in configuration.")
  }

  /**
   * Return an Entity proto
   */
  def toEntity(name: String, types: List[String], extraTypes: List[String]): Entity = {
    val protoBuilder = Entity.newBuilder.setName(name)

    extraTypes.foreach { extra =>
      if (types.contains(extra))
        println("DEPRECATION WARNING: \"" + extra + "\" type is automatically added to " + name)
    }

    val finalTypes = (types ::: extraTypes).distinct
    finalTypes.foreach(typ => protoBuilder.addTypes(typ))

    //profiles.foreach( p => p.getType.toList.foreach(typ => proto.addTypes(typ.getName)) )
    //profiles.foreach( p => applyEquipmentTypes( proto, p))

    protoBuilder.build
  }

  /**
   * Commands are controls and setpoints.
   */
  def toCommand(name: String, displayName: String, entity: Entity, commandType: CommandTypeProto): CommandProto = {
    val builder = CommandProto.newBuilder
      .setName(name)
      .setDisplayName(displayName)
      .setEntity(entity)
      .setType(commandType)

    builder.build
  }

  private def convertSetpointTypeToCommandType(setpointType: SetpointType): CommandTypeProto = {
    setpointType match {
      case SetpointType.DOUBLE => CommandTypeProto.SETPOINT_DOUBLE
      case SetpointType.STRING => CommandTypeProto.SETPOINT_STRING
      case SetpointType.INTEGER => CommandTypeProto.SETPOINT_INT
      case _ => throw new LoadingException("Unknown setpointType: " + setpointType)
    }
  }
}

