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
import org.totalgrid.reef.loader.communications._
import org.totalgrid.reef.api.proto.FEP._
import org.totalgrid.reef.api.proto.Processing._
import com.weiglewilczek.slf4s.Logging
import java.io.File
import org.totalgrid.reef.api.proto._

import EnhancedXmlClasses._
import org.totalgrid.reef.loader.common.ConfigFile

object CommunicationsLoader {
  val MAPPING_STATUS = Mapping.DataType.valueOf("BINARY")
  val MAPPING_ANALOG = Mapping.DataType.valueOf("ANALOG")
  val MAPPING_COUNTER = Mapping.DataType.valueOf("COUNTER")
  val BENCHMARK = "benchmark"
  val DNP3 = "dnp3"
  val DNP3Slave = "dnp3-slave"
  val MODBUS = "modbus"
}

/**
 * Loader for the communications model.
 *
 * TODO: Add serial interfaces - backlog-65
 *
 */
class CommunicationsLoader(modelLoader: ModelLoader, loadCache: LoadCacheCommunication, exceptionCollector: ExceptionCollector,
  commonLoader: CommonLoader)
    extends Logging with BaseConfigurationLoader {

  val controlProfiles = LoaderMap[ControlProfile]("Control Profile")
  val pointProfiles = LoaderMap[PointProfile]("Point Profile")
  val endpointProfiles = LoaderMap[EndpointType]("Endpoint Profile")
  val equipmentProfiles = LoaderMap[EquipmentType]("Equipment Profile")

  val interfaces = LoaderMap[Interface]("Interface (port)")

  /**
   * Reset all class variables
   */
  def reset: Unit = {
    controlProfiles.clear()
    pointProfiles.clear()
    endpointProfiles.clear()
    equipmentProfiles.clear()
    interfaces.clear()
  }

  def getExceptionCollector: ExceptionCollector = {
    exceptionCollector
  }

  def getModelLoader: ModelLoader = {
    modelLoader
  }

  /**
   * Load this equipment node and all children. Create edges to connect the children.
   */
  def load(model: CommunicationsModel, equipmentPointUnits: HashMap[String, String]): Unit = {
    load(model, equipmentPointUnits, false)
  }

  /**
   * Load this equipment node and all children. Create edges to connect the children.
   * if benchmark is true ignore endpoint type and use benchmark protocol
   */
  def load(model: CommunicationsModel, equipmentPointUnits: HashMap[String, String], benchmark: Boolean) = {
    logger.info("Start")

    // Collect all the profiles in name->profile maps.
    model.getProfiles.foreach { profiles =>
      profiles.getControlProfile.toList.foreach(controlProfile => controlProfiles += (controlProfile.getName -> controlProfile))
      profiles.getPointProfile.toList.foreach(pointProfile => pointProfiles += (pointProfile.getName -> pointProfile))
      profiles.getEndpointProfile.toList.foreach(endpointProfile => endpointProfiles += (endpointProfile.getName -> endpointProfile))
      profiles.getEquipmentProfile.toList.foreach(equipmentProfile => equipmentProfiles += (equipmentProfile.getName -> equipmentProfile))
    }
    logger.info("Loading Communications: found ControlProfiles: " + controlProfiles.keySet.mkString(", "))
    logger.info("Loading Communications: found PointProfiles: " + pointProfiles.keySet.mkString(", "))
    logger.info("Loading Communications: found EndpointProfiles: " + endpointProfiles.keySet.mkString(", "))
    logger.info("Loading Communications: found EquipmentProfiles: " + equipmentProfiles.keySet.mkString(", "))

    validateProfiles

    model.getInterface.toList.foreach(interface => interfaces += (interface.getName -> interface))
    logger.info("Loading Communications: found Interfaces: " + interfaces.keySet.mkString(", "))

    // Load endpoints
    model.getEndpoint.toList.foreach(e => {
      println("Loading Communications: processing endpoint '" + e.getName + "'")
      exceptionCollector.collect("Endpoint: " + e.getName) {
        loadEndpoint(e, equipmentPointUnits, benchmark)
      }
    })

    logger.info("End")
  }

  /**
   * Validate the various profiles before we start using them.
   */
  def validateProfiles: Unit = {
    import ProtoUtils.validatePointScale

    //  pointProfiles: scale
    for ((name, profile) <- pointProfiles) {
      if (profile.isSetScale)
        validatePointScale(exceptionCollector, "profile '" + name + "'", profile.getScale)
    }

    // TODO: more profile validation ...

  }

  /**
   * Load this endpoint node and all children. Create edges to connect the children.
   */
  def loadEndpoint(endpoint: Endpoint, equipmentPointUnits: HashMap[String, String], benchmark: Boolean): Unit = {
    import CommunicationsLoader._

    val endpointName = endpoint.getName
    logger.trace("loadEndpoint: " + endpointName)

    // IMPORTANT:  profiles is a list of profiles plus this endpoint (as the last "profile" in the list)
    //
    val profiles: List[EndpointType] = endpoint.getEndpointProfile.toList.map(p => endpointProfiles(p.getName)) ::: List[EndpointType](endpoint)
    logger.info("load endpoint '" + endpointName + "' with profiles: " + profiles.map(_.getName).dropRight(1).mkString(", ")) // don't print last profile which is this endpoint

    //var (protocol, configFiles) = processProtocol(profiles, path: File, benchmark)

    val protocol = findProtocol(profiles)
    var configFiles = commonLoader.loadConfigFiles(protocol.getConfigFile.toList)

    val originalProtocolName = protocol.getName
    val overriddenProtocolName = if (benchmark) BENCHMARK else originalProtocolName

    val commChannel = processInterface(profiles)

    //  Walk the tree of equipment nodes to collect the controls and points
    // An endpoint may have multiple top level equipment objects (each with nested equipment nodes).
    val controls = HashMap[String, Control]()
    val setpoints = HashMap[String, Setpoint]()
    val statuses = HashMap[String, PointType]()
    val analogs = HashMap[String, PointType]()
    val counters = HashMap[String, PointType]()

    profiles.flatMap(_.getEquipment).foreach(findControlsAndPoints(_, None, controls, setpoints, statuses, analogs, counters))
    logger.trace("loadEndpoint: " + endpointName + " with controls: " + controls.keys.mkString(", "))
    logger.trace("loadEndpoint: " + endpointName + " with setpoints: " + setpoints.keys.mkString(", "))
    logger.trace("loadEndpoint: " + endpointName + " with statuses: " + statuses.keys.mkString(", "))
    logger.trace("loadEndpoint: " + endpointName + " with analogs: " + analogs.keys.mkString(", "))
    logger.trace("loadEndpoint: " + endpointName + " with counters: " + counters.keys.mkString(", "))

    if (endpoint.isDataSource) {
      for ((name, c) <- controls) loadCache.addControl(endpointName, name, if (c.isSetIndex) c.getIndex else -1)
      for ((name, s) <- setpoints) loadCache.addControl(endpointName, name, if (s.isSetIndex) s.getIndex else -1)
    }

    // Validate that the indexes within each type are unique
    val errorMsg = "Endpoint '" + endpointName + "':"
    val isBenchmark = overriddenProtocolName == BENCHMARK
    logger.debug("checking indices: isBenchmark: " + isBenchmark + ", overridden protocol name: " + overriddenProtocolName)
    exceptionCollector.collect("Checking Indexes: " + endpointName) {
      validateIndexesAreUnique[Control](controls, isBenchmark, errorMsg, compareControls)
      validateIndexesAreUnique[Setpoint](setpoints, isBenchmark, errorMsg)
      validateIndexesAreUnique[PointType](statuses, isBenchmark, errorMsg)
      validateIndexesAreUnique[PointType](analogs, isBenchmark, errorMsg)
      validateIndexesAreUnique[PointType](counters, isBenchmark, errorMsg)
    }

    // Collect all the point types into points while making sure each name is unique across all point types.
    val points = HashMap[String, PointType]()
    for ((name, p) <- statuses) addUniquePoint(points, name, p, errorMsg + "status")
    for ((name, p) <- analogs) addUniquePoint(points, name, p, errorMsg + "analog")
    for ((name, p) <- counters) addUniquePoint(points, name, p, errorMsg + "counter")
    logger.trace("loadEndpoint: " + endpointName + " with all points: " + points.keys.mkString(", "))

    processPointScaling(endpointName, points, equipmentPointUnits, endpoint.isDataSource)

    // TODO should the communications loader really have knowledge of all the protocols?
    overriddenProtocolName match {
      case DNP3 | DNP3Slave | MODBUS =>
        exceptionCollector.collect("DNP3 Indexes:" + endpointName) {
          configFiles ::= processIndexMapping(endpointName, controls, setpoints, points)
        }
        if (commChannel.isEmpty)
          throw new LoadingException("Endpoint '" + endpointName + "' has no interface element specified.")
      case BENCHMARK => {
        val delay = protocol.getSimOptions.map { _.getDelay }
        exceptionCollector.collect("Simulator Mapping:" + endpointName) {
          configFiles ::= createSimulatorMapping(endpointName, controls, setpoints, points, delay)
        }
      }
      case _ => // do nothing
    }

    // Now we have a list of all the controls and points for this Endpoint
    val endpointCfg = toCommunicationEndpointConfig(endpointName, overriddenProtocolName, configFiles, commChannel, controls, setpoints,
      points, endpoint.isDataSource).build
    modelLoader.putOrThrow(endpointCfg)

    val endpointEntity = ProtoUtils.toEntityType(endpointName, "CommunicationEndpoint" :: Nil)
    endpoint.getInfo.foreach(info => commonLoader.addInfo(endpointEntity, info))
  }

  private def compareControls(a: Control, b: Control): Boolean = {
    if (a.isSetOptionsDnp3 != b.isSetOptionsDnp3) return false
    if (!a.isSetOptionsDnp3) return false

    val optA = a.getOptionsDnp3.get
    val optB = b.getOptionsDnp3.get

    return (optA.isSetCount == optB.isSetCount && (!optA.isSetCount || optA.getCount == optB.getCount)) &&
      (optA.isSetType == optB.isSetType && (!optA.isSetType || optA.getType == optB.getType)) &&
      (optA.isSetOnTime == optB.isSetOnTime && (!optA.isSetOnTime || optA.getOnTime == optB.getOnTime)) &&
      (optA.isSetOffTime == optB.isSetOffTime && (!optA.isSetOffTime || optA.getOffTime == optB.getOffTime))
  }

  /**
   * The indexes for controls and points have to be unique for each type.
   */
  def validateIndexesAreUnique[A <: IndexType](indexables: HashMap[String, A], isBenchmark: Boolean, error: String, equalsFunc: (A, A) => Boolean = { (a: A, b: A) => true }) {
    val map = HashMap[Int, A]()
    for ((name, indexable) <- indexables) {
      // if there are indexes, check them.  if the originalProtocol is benchmark, indexes are optional
      if (indexable.isSetIndex) {
        val index = indexable.getIndex
        map.contains(index) match {
          case true =>
            if (equalsFunc(indexable, map(index))) {
              val msg: String = error + " both '" + name + "' and '" + map(index).getName + "' cannot use the same index=\"" + index + "\""
              logger.error(msg)
              throw new LoadingException(msg)
            }
          case false => map += (index -> indexable)
        }
      } else {
        if (!isBenchmark) {
          val msg: String = error + " '" + name + "' does not specify an index."
          logger.error(msg)
          throw new LoadingException(msg)
        }
      }
    }
  }

  /**
   * Add this point to the points map and check that the name is unique.
   */
  def addUniquePoint(points: HashMap[String, PointType], name: String, point: PointType, error: String): Unit = {

    points.contains(name) match {
      case true => throw new LoadingException(error + " name=\"" + name + "\" cannot be the same name as another point in this endpoint.")
      case false => points += (name -> point)
    }
  }

  /**
   * The endppoint may have a fully populated protocol or maybe just an protocol name.
   * One of the endpointProfiles may have an protocol defined.
   */
  def findProtocol(profiles: List[EndpointType]): Protocol = {
    val endpointName = profiles.last.getName

    // Walk the endpointTypes backwards to find the first protocol element
    val protocol: Protocol = profiles.reverse.find(_.isSetProtocol) match {
      case Some(endpoint) => endpoint.getProtocol.get
      case None =>
        profiles.size match {
          case 1 => throw new LoadingException("Endpoint '" + endpointName + "' has no protocol element specified.")
          case _ => throw new LoadingException("Endpoint '" + endpointName + "' and its associated endpointProfile element(s) do not specify a protocol.")
        }
    }
    if (!protocol.isSetName)
      throw new LoadingException("Endpoint '" + endpointName + "' has a protocol element without a name attribute (ex: <protocol name=\"benchmark\"/>).")

    protocol
  }

  //  def processConfigFiles(configFiles: List[ConfigFile], path: File): List[Model.ConfigFile.Builder] = {
  //    var cfs = List.empty[Model.ConfigFile.Builder]
  //    ex.collect("Loading config files for endpoint: ") {
  //      val configs = configFiles.toList.map(c => commonLoader.toConfigFile(c))
  //      configs.foreach(cf => client.putOrThrow(cf))
  //      cfs = configs.map(_.toBuilder)
  //    }
  //    cfs
  //  }

  /**
   * The endpoint may have an interface via:
   *    - A fully populated interface in this endpoint (i.e. has all attributes)
   *    - A named interface that references a globally defined interface
   *    - An interface defined in an included profile (which could be a
   *      named interface that references a globally defined interface).
   *
   * For each interface attribute, look first in the interface's attributes and,
   * if not found, look in the globally referencedinterface. In this way, the
   * endpoint's interface can override individual properties of the globally
   * defined interface.
   *
   * Multiple Profiles:
   * There may be multiple profiles each with an interface. The profile.interface picked is
   * first one found by search backwards through the list of profiles.
   *
   */
  def processInterface(profiles: List[EndpointType]): Option[CommChannel.Builder] = {

    val endpointName = profiles.last.getName
    // Walk the endpointTypes backwards to find the first interface specified. The list of profiles
    // includes this endpoint as the last "profile".
    val interfaceElement = profiles.reverse.find(_.isSetInterface).map { _.getInterface.get }

    interfaceElement.map { interface =>
      // If the interface found inside the endpoint or endpoint profile specifies
      // all the required attributes, it doesn't need a name.
      val reference = interface.isSetName match {
        case true => interfaces.get(interface.getName) // get the named interface if it exists.
        case false => None
      }

      // Get each attribute from the endpoint's interface or the endpoint profile's
      // interface. The endpoint's interface can override individual properties
      // of the reference interface

      val server = getAttributeDefault[Boolean](interface, reference, _.isSetServerSocket, _.isServerSocket, false)

      val ip = if (!server) getAttribute[String](interface, reference, _.isSetIp, _.getIp, endpointName, "ip")
      else getAttributeDefault[String](interface, reference, _.isSetIp, _.getIp, "0.0.0.0")
      val port = getAttribute[Int](interface, reference, _.isSetPort, _.getPort, endpointName, "port")
      val network = getAttributeDefault[String](interface, reference, _.isSetNetwork, _.getNetwork, "any")

      val ipProto = IpPort.newBuilder
        .setAddress(ip)
        .setPort(port)
        .setNetwork(network)
        .setMode(if (server) IpPort.Mode.SERVER else IpPort.Mode.CLIENT)

      val portProto = CommChannel.newBuilder
        .setName("tcp://" + ip + ":" + port + "@" + network)
        .setIp(ipProto)
        .build

      modelLoader.putOrThrow(portProto)

      portProto.toBuilder
    }
  }

  /**
   * Get an attribute in the interface or it's named reference interface
   * Throw exception if no attribute found.
   */
  def getAttribute[A](
    interface: Interface,
    reference: Option[Interface],
    isSet: (Interface) => Boolean,
    get: (Interface) => A,
    endpointName: String,
    attributeName: String): A = {

    val value: A = isSet(interface) match {
      case true => get(interface)
      case false =>
        reference match {
          case Some(i) => get(i)
          case _ => throw new LoadingException("Endpoint '" + endpointName + "' interface is missing required '" + attributeName + "' attribute.")
        }
    }
    value
  }

  /**
   * Get an attribute in the interface or it's named reference interface
   * Return default value if no attribute found.
   */
  def getAttributeDefault[A](
    interface: Interface,
    reference: Option[Interface],
    isSet: (Interface) => Boolean,
    get: (Interface) => A,
    default: A): A = {
    (isSet(interface), reference.map { isSet(_) }.getOrElse(false)) match {
      case (true, _) => get(interface)
      case (false, true) => get(reference.get)
      case _ => default
    }
  }

  def processIndexMapping(
    endpointName: String,
    controls: HashMap[String, Control],
    setpoints: HashMap[String, Setpoint],
    points: HashMap[String, PointType]): Model.ConfigFile = {

    logger.debug(endpointName + " CONTROLS:")
    val controlProtos = for ((key, value) <- controls) yield controlToCommandMap(endpointName, key, value)

    logger.debug(endpointName + " SETPOINTS:")
    val setpointsProtos = for ((key, value) <- setpoints) yield setpointToCommandMap(endpointName, key, value)

    logger.debug(endpointName + " POINTS:")
    val pointProtos = for ((key, value) <- points) yield toMeasMap(endpointName, key, value)

    val indexMap = toIndexMapping(controlProtos.toList ::: setpointsProtos.toList, pointProtos).build

    val configFile = toConfigFile(endpointName, indexMap).build
    modelLoader.putOrThrow(configFile)
    configFile
  }

  /**
   * Process point scaling. Could be other stuff in the future.
   * TODO: Can't get some attributes from point/scale and some from pointProfile/scale.
   */
  def processPointScaling(endpointName: String, points: HashMap[String, PointType], equipmentPointUnits: HashMap[String, String], isDataSource: Boolean): Unit = {
    import ProtoUtils._

    for ((name, point) <- points) exceptionCollector.collect("Point: " + endpointName + "." + name) {
      val profile = getPointProfile(endpointName, point)
      val scale = if (point.isSetScale) // point/scale overrides any pointProfile/scale.
        Some(point.getScale)
      else if (profile.isDefined && profile.get.isSetScale)
        Some(profile.get.getScale)
      else
        None

      val index = if (point.isSetIndex) point.getIndex else -1

      val unit = if (scale.isDefined) {
        val s = scale.get

        if (!s.isSetEngUnit)
          throw new LoadingException("Endpoint '" + endpointName + "': <scale> element used by point '" + name + "' does not have required attribute 'engUnit'")

        val unit = s.getEngUnit

        equipmentPointUnits.get(name) match {
          case Some(u) =>
            if (unit != u)
              throw new LoadingException("Endpoint '" + endpointName + "': <scale ... engUnit=\"" + unit + "\"/> does not match point '" + name + "' unit=\"" + u + "\" in equipment model.")
          case _ => // OK: the equipment point doesn't have to be in this config file.
        }

        val point = toPoint(name)

        addTriggers(modelLoader, point, toTrigger(name, s) :: Nil)

        unit
      } else {
        if (!point.isSetUnit) "" else point.getUnit
      }
      if (isDataSource) loadCache.addPoint(endpointName, name, index, unit)
    }
  }

  def getPointProfile(elementName: String, point: PointType): Option[PointProfile] = {
    if (point.isSetPointProfile) {
      val p = point.getPointProfile
      if (pointProfiles.contains(p))
        Some(pointProfiles(p))
      else
        throw new LoadingException("pointProfile '" + p + "' referenced from '" + elementName + "' was not found in configuration.")
    } else
      None
  }

  /**
   * Recursively find all controls and points nested in this equipment.
   * This equipment may have:
   *  equipmentProfile
   *  control
   *  status, analog, pointer
   *  equipment
   */
  def findControlsAndPoints(equipment: Equipment,
    namePrefix: Option[String],
    controls: HashMap[String, Control],
    setpoints: HashMap[String, Setpoint],
    statuses: HashMap[String, PointType],
    analogs: HashMap[String, PointType],
    counters: HashMap[String, PointType]): Unit = {

    val name = getChildName(namePrefix, equipment.getName)
    val childPrefix = if (equipment.isAddParentNames) Some(name + ".") else None
    logger.trace("findControlAndPoints: " + name)

    // IMPORTANT:  profiles is a list of profiles plus this equipment (as the last "profile" in the list)
    //
    val profiles: List[EquipmentType] = equipment.getEquipmentProfile.toList ::: List[EquipmentType](equipment)

    profiles.flatMap(_.getControl).foreach(c => addUnique[Control](controls, getChildName(childPrefix, c.getName), c, "Duplicate control name: "))
    profiles.flatMap(_.getSetpoint).foreach(c => addUnique[Setpoint](setpoints, getChildName(childPrefix, c.getName), c, "Duplicate setpoint name: "))
    profiles.flatMap(_.getStatus).foreach(p => addUnique[PointType](statuses, getChildName(childPrefix, p.getName), p, "Duplicate status name: "))
    profiles.flatMap(_.getAnalog).foreach(p => addUnique[PointType](analogs, getChildName(childPrefix, p.getName), p, "Duplicate analog name: "))
    profiles.flatMap(_.getCounter).foreach(p => addUnique[PointType](counters, getChildName(childPrefix, p.getName), p, "Duplicate counter name: "))

    // Recurse into child equipment
    profiles.flatMap(_.getEquipment).foreach(findControlsAndPoints(_, childPrefix, controls, setpoints, statuses, analogs, counters))
  }

  def addUnique[A](map: HashMap[String, A], key: String, indexable: A, error: String): Unit = {
    if (map.contains(key))
      throw new LoadingException(error + "'" + key + "'. Point names need to be unique, even across different point types.")
    map += (key -> indexable)
  }

  /**
   * Return true if there are no duplicate names,
   */
  def isNameDistinct(indexables: List[IndexType]) =
    indexables.map(_.getName).distinct.size == indexables.size

  def toCommunicationEndpointConfig(
    name: String,
    protocol: String,
    configFiles: List[Model.ConfigFile],
    port: Option[CommChannel.Builder],
    controls: HashMap[String, Control],
    setpoints: HashMap[String, Setpoint],
    points: HashMap[String, PointType],
    isDataSource: Boolean): CommEndpointConfig.Builder = {

    val proto = CommEndpointConfig.newBuilder
      .setName(name)
      .setProtocol(protocol)
      .setOwnerships(toEndpointOwnership(controls.keys.toList ::: setpoints.keys.toList, points.keys))

    proto.setDataSource(isDataSource)

    if (port.isDefined)
      proto.setChannel(port.get)

    configFiles.foreach(proto.addConfigFiles)

    proto
  }

  def toEndpointOwnership(commands: Iterable[String], points: Iterable[String]): EndpointOwnership.Builder = {
    val proto = EndpointOwnership.newBuilder

    commands.foreach(proto.addCommands(_))
    points.foreach(proto.addPoints(_))

    proto
  }

  /**
   * Store an indexMapping proto inside a ConfigFile proto.
   */
  def toConfigFile(name: String, indexMapping: Mapping.IndexMapping): Model.ConfigFile.Builder = {
    val proto = Model.ConfigFile.newBuilder
      .setName(name + "-mapping.pi")
      .setMimeType("application/vnd.google.protobuf; proto=reef.api.proto.Mapping.IndexMapping")
      .setFile(indexMapping.toByteString)

    proto
  }

  def toIndexMapping(
    commands: Iterable[Mapping.CommandMap.Builder],
    points: Iterable[Mapping.MeasMap.Builder]): Mapping.IndexMapping.Builder = {

    val proto = Mapping.IndexMapping.newBuilder
    commands.foreach(proto.addCommandmap)
    points.foreach(proto.addMeasmap)
    proto
  }

  def toMeasMap(endpointName: String, name: String, point: PointType): Mapping.MeasMap.Builder =
    {
      import CommunicationsLoader._

      if (!point.isSetIndex) {
        throw new LoadingException("ERROR in endpoint '" + endpointName + "' - Point '" + name + "' has no index specified.")
      }

      logger.debug("    POINT: " + point.getIndex + " -> " + name)
      val builder = Mapping.MeasMap.newBuilder.setPointName(name).setUnit(point.getUnit)
      builder.setIndex(point.getIndex)

      val pointType = point match {
        case status: Status => MAPPING_STATUS
        case analog: Analog => MAPPING_ANALOG
        case counter: Counter => MAPPING_COUNTER
      }
      builder.setType(pointType)

      builder
    }

  def setpointToCommandMap(endpointName: String, name: String, setpoint: Setpoint): Mapping.CommandMap.Builder = {

    // Profiles is a list of profiles plus this setpoint
    val profiles: List[ControlType] = setpoint.getControlProfile.toList.map(p => controlProfiles(p.getName)) ::: List[ControlType](setpoint)
    val reverseProfiles = profiles.reverse

    // Search the reverse profile list to fInd an index
    val index = reverseProfiles.find(_.isSetIndex) match {
      case Some(ct) => ct.getIndex
      case None =>
        reverseProfiles.size match {
          case 1 => throw new LoadingException("Command '" + name + "' has no index specified.")
          case _ => throw new LoadingException("Command '" + name + "' and its associated controlProfile element(s) do not specify an index.")
        }
    }

    logger.debug("    COMMAND " + index + " -> " + name)

    Mapping.CommandMap.newBuilder
      .setCommandName(name)
      .setIndex(index)
      .setType(Mapping.CommandType.SETPOINT)

  }

  def controlToCommandMap(endpointName: String, name: String, control: Control): Mapping.CommandMap.Builder = {

    // Profiles is a list of profiles plus this control
    val profiles: List[ControlType] = control.getControlProfile.toList.map(p => controlProfiles(p.getName)) ::: List[ControlType](control)
    val reverseProfiles = profiles.reverse

    // Search the reverse profile list to find an index
    val index = reverseProfiles.find(_.isSetIndex) match {
      case Some(ct) => ct.getIndex
      case None =>
        reverseProfiles.size match {
          case 1 => throw new LoadingException("Control '" + name + "' has no index specified.")
          case _ => throw new LoadingException("Control '" + name + "' and its associated controlProfile element(s) do not specify an index.")
        }
    }
    logger.debug("    COMMAND " + index + " -> " + name)
    // Find the first optionsDNP3 in the reverse profile list.
    // TODO: We get all attributes from a single optionsDnp3. Could "find" each attribute in the first optionsDnp3 that has that attribute.
    val options: OptionsDnp3 = reverseProfiles.find(_.isSetOptionsDnp3) match {
      case Some(ct) => ct.getOptionsDnp3.get
      case None =>
        reverseProfiles.size match {
          case 1 => throw new LoadingException("Control '" + name + "' has no optionsDnp3 element specified.")
          case _ => throw new LoadingException("Control '" + name + "' and its associated controlProfile element(s) do not specify an optionsDnp3 element.")
        }
    }

    val proto = Mapping.CommandMap.newBuilder
      .setCommandName(name)
      .setIndex(index)
      .setType(Mapping.CommandType.valueOf(options.getType))

    if (options.isSetOnTime)
      proto.setOnTime(options.getOnTime)
    if (options.isSetOffTime)
      proto.setOffTime(options.getOffTime)
    if (options.isSetCount)
      proto.setCount(options.getCount)

    proto
  }

  def createSimulatorMapping(
    endpointName: String,
    controls: HashMap[String, Control],
    setpoints: HashMap[String, Setpoint],
    points: HashMap[String, PointType],
    delay: Option[Int]): Model.ConfigFile = {

    val controlProtos = for ((key, value) <- controls) yield toCommandSim(key).build
    val setpointProtos = for ((key, value) <- setpoints) yield toCommandSim(key).build
    val pointProtos = for ((key, value) <- points) yield toMeasSim(key, value).build

    val simMap = SimMapping.SimulatorMapping.newBuilder

    simMap.setDelay(delay getOrElse 500)

    simMap.addAllMeasurements(pointProtos.toList)
    simMap.addAllCommands(controlProtos.toList ::: setpointProtos.toList)

    val configFile = toConfigFile(endpointName, simMap.build).build
    modelLoader.putOrThrow(configFile)
    configFile
  }

  def toCommandSim(name: String): SimMapping.CommandSim.Builder = {
    SimMapping.CommandSim.newBuilder.setName(name).setResponseStatus(Commands.CommandStatus.SUCCESS)
  }

  def toMeasSim(name: String, point: PointType): SimMapping.MeasSim.Builder = {
    import ProtoUtils._

    logger.debug("SIM POINT -> " + name)
    val builder = SimMapping.MeasSim.newBuilder.setName(name).setUnit(point.getUnit)

    var triggerSet = modelLoader.getOrThrow(toTriggerSet(toPoint(name))).headOption

    var inBoundsRatio = 0.85
    var changeChance = 1.0

    point match {
      case status: Status =>
        builder.setType(Measurements.Measurement.Type.BOOL)
        val boolTrigger = triggerSet.map { _.getTriggersList.find(_.hasBoolValue) }.flatMap { x => x }
        val bval = boolTrigger.map { _.getBoolValue }.getOrElse(false)
        builder.setInitial(if (bval) 1 else 0)
        changeChance = 0.03
      case analog: Analog =>
        builder.setType(Measurements.Measurement.Type.DOUBLE)
        configureNumericMeasSim(builder, triggerSet, inBoundsRatio)
      case counter: Counter =>
        builder.setType(Measurements.Measurement.Type.INT)
        configureNumericMeasSim(builder, triggerSet, inBoundsRatio)
        builder.setMaxDelta(builder.getMaxDelta.max(1.0))
    }
    builder.setChangeChance(changeChance)

    builder
  }

  def configureNumericMeasSim(proto: SimMapping.MeasSim.Builder, triggerSet: Option[TriggerSet], inBoundsRatio: Double) {
    val firstTriggerOption: Option[Option[Processing.AnalogLimit]] = triggerSet.map { _.getTriggersList.find(_.hasAnalogLimit).map { _.getAnalogLimit } }

    val firstTrigger = firstTriggerOption.flatMap { x => x }

    val min = firstTrigger.map { _.getLowerLimit }.getOrElse(-50.0)
    val max = firstTrigger.map { _.getUpperLimit }.getOrElse(50.0)

    val range = max - min

    val simRange = range / inBoundsRatio
    val middle = range / 2 + min

    proto.setMaxDelta(simRange / 50)
    proto.setMin(middle - simRange / 2)
    proto.setMax(middle + simRange / 2)
    proto.setInitial(middle)
  }

  def toConfigFile(endpointName: String, simMapping: SimMapping.SimulatorMapping): Model.ConfigFile.Builder = {
    val configFileBuilder = Model.ConfigFile.newBuilder
      .setName(endpointName + "-sim.pi")
      .setMimeType("application/vnd.google.protobuf; proto=reef.api.proto.SimMapping.SimulatorMapping")
      .setFile(simMapping.toByteString)

    logger.info("simulator mapping: endpoint: " + endpointName + ", mapping: " + simMapping.toString)

    configFileBuilder
  }
}
