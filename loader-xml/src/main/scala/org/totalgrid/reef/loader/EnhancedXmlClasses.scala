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

object MixedClassListHelpers {
  def withClass[T <: AnyRef](list: List[Any], clazz: Class[T]): List[T] = {
    list.filter { clazz.isInstance(_) }.map { _.asInstanceOf[T] }
  }

  def one[T <: AnyRef](list: List[Any], clazz: Class[T]): Option[T] = {
    withClass(list, clazz) match {
      case List(one) => Some(one)
      case Nil => None
      case _ => throw new LoadingException("More than one " + clazz.getSimpleName + " defined, should be 0 or 1")
    }
  }
}
import MixedClassListHelpers._

object EnhancedEquipmentElements {
  import org.totalgrid.reef.loader.equipment._
  class EnhancedEquipment(equipment: EquipmentType) {

    lazy val parts = equipment.getEquipmentProfileOrTypeOrControl.toList

    def getEquipmentProfile = withClass(parts, classOf[EquipmentProfile])
    def getEquipment = withClass(parts, classOf[Equipment])
    def getControl = withClass(parts, classOf[Control])
    def getSetpoint = withClass(parts, classOf[Setpoint])
    def getStatus = withClass(parts, classOf[Status])
    def getAnalog = withClass(parts, classOf[Analog])
    def getCounter = withClass(parts, classOf[Counter])
    def getType = withClass(parts, classOf[Type])
    def getInfo = one(parts, classOf[common.Info])
  }

  class EnhancedPointType(pt: PointType) {

    lazy val parts = pt.getTransformOrControlOrSetpoint.toList

    def getType = withClass(parts, classOf[Type])
    def getRange = withClass(parts, classOf[Range])
    def getUnexpected = withClass(parts, classOf[Unexpected])
    def getFilter = withClass(parts, classOf[Filter])
    def getControl = withClass(parts, classOf[Control])
    def getTransform = withClass(parts, classOf[Transform])
    def getSetpoint = withClass(parts, classOf[Setpoint])
    def getInfo = one(parts, classOf[common.Info])

    import org.totalgrid.reef.loader.calculations.Calculation
    def getCalculation = withClass(parts, classOf[Calculation])
  }

  class EnhancedCommand(pt: Command) {

    lazy val parts = pt.getTypeOrInfo.toList

    def getType = withClass(parts, classOf[Type])
    def getInfo = one(parts, classOf[common.Info])
  }

  class EnhancedProfiles(pt: Profiles) {

    lazy val parts = pt.getPointProfileOrEquipmentProfile.toList

    def getPointProfile = withClass(parts, classOf[PointProfile])
    def getEquipmentProfile = withClass(parts, classOf[EquipmentProfile])
  }
}

object EnhancedCommunicationElements {
  import org.totalgrid.reef.loader.communications._
  class EnhancedEquipment(equipment: EquipmentType) {

    val parts = equipment.getEquipmentProfileOrControlOrSetpoint.toList

    def getEquipmentProfile = withClass(parts, classOf[EquipmentProfile])
    def getEquipment = withClass(parts, classOf[Equipment])
    def getControl = withClass(parts, classOf[Control])
    def getSetpoint = withClass(parts, classOf[Setpoint])
    def getStatus = withClass(parts, classOf[Status])
    def getAnalog = withClass(parts, classOf[Analog])
    def getCounter = withClass(parts, classOf[Counter])
    def getType = withClass(parts, classOf[Type])
    def getInfo = one(parts, classOf[common.Info])
  }

  class EnhancedProfiles(pt: Profiles) {

    lazy val parts = pt.getControlProfileOrPointProfileOrEndpointProfile.toList

    def getPointProfile = withClass(parts, classOf[PointProfile])
    def getControlProfile = withClass(parts, classOf[ControlProfile])
    def getEquipmentProfile = withClass(parts, classOf[EquipmentProfile])
    def getEndpointProfile = withClass(parts, classOf[EndpointProfile])
  }

  class EnhancedEndpointType(e: EndpointType) {
    lazy val parts = e.getProtocolOrInterfaceOrEndpointProfile.toList

    def getEndpointProfile = withClass(parts, classOf[EndpointProfile])
    def getEquipment = withClass(parts, classOf[Equipment])
    def getConfigFile = withClass(parts, classOf[common.ConfigFile])

    def isSetProtocol = withClass(parts, classOf[Protocol]).size > 0
    def getProtocol = one(parts, classOf[Protocol])

    def isSetInterface = withClass(parts, classOf[Interface]).size > 0
    def getInterface = one(parts, classOf[Interface])

    def getInfo = one(parts, classOf[common.Info])
  }

  class EnhancedCommunicationsModel(cm: CommunicationsModel) {
    lazy val parts = cm.getProfilesOrInterfaceOrEndpoint.toList

    def getProfiles = withClass(parts, classOf[Profiles])
    def getEndpoint = withClass(parts, classOf[Endpoint])
    def getInterface = withClass(parts, classOf[Interface])
  }

  class EnhancedControlType(c: ControlType) {
    lazy val parts = c.getControlProfileOrOptionsDnp3.toList

    def getControlProfile = withClass(parts, classOf[ControlProfile])

    def isSetOptionsDnp3 = withClass(parts, classOf[OptionsDnp3]).size > 0
    def getOptionsDnp3 = one(parts, classOf[OptionsDnp3])
  }

  class EnhancedProtocol(c: Protocol) {
    lazy val parts = c.getSimOptionsOrConfigFile.toList

    def getConfigFile = withClass(parts, classOf[common.ConfigFile])

    def isSetSimOptions = withClass(parts, classOf[SimOptions]).size > 0
    def getSimOptions = one(parts, classOf[SimOptions])
  }
}

object EnhancedInfoElements {
  import org.totalgrid.reef.loader.common._

  class EnhancedInfo(c: Info) {
    lazy val parts = c.getConfigFileOrAttribute.toList

    def getConfigFile = withClass(parts, classOf[ConfigFile])
    def getAttribute = withClass(parts, classOf[Attribute])

  }

  class EnhancedAttribute(c: Attribute) {
    def doubleValue = if (c.isSetDoubleValue) Some(c.getDoubleValue) else None
    def intValue = if (c.isSetIntValue) Some(c.getIntValue) else None
    def booleanValue = if (c.isSetBooleanValue) Some(c.isBooleanValue) else None
    def stringValue = if (c.isSetStringValue) Some(c.getStringValue) else None
  }
}

/**
 * the enhanced xml classes are necessary to paper over the fact we use xs:choice unbounded vs xs:sequence
 * in the schema. If we use sequences and "mixed-use" containers we get very confusing validation errors that
 * are due to putting elements in the wrong order.
 */
object EnhancedXmlClasses {
  implicit def enhanceEquEquipment(et: equipment.EquipmentType) = new EnhancedEquipmentElements.EnhancedEquipment(et)
  implicit def enhanceEquPointType(et: equipment.PointType) = new EnhancedEquipmentElements.EnhancedPointType(et)
  implicit def enhanceEquCommand(et: equipment.Command) = new EnhancedEquipmentElements.EnhancedCommand(et)
  implicit def enhanceEquProfiles(et: equipment.Profiles) = new EnhancedEquipmentElements.EnhancedProfiles(et)

  implicit def enhanceCommsEquipment(et: communications.EquipmentType) = new EnhancedCommunicationElements.EnhancedEquipment(et)
  implicit def enhanceCommsProfiles(et: communications.Profiles) = new EnhancedCommunicationElements.EnhancedProfiles(et)
  implicit def enhanceCommsEndpointType(et: communications.EndpointType) = new EnhancedCommunicationElements.EnhancedEndpointType(et)
  implicit def enhanceCommsCommunicationModel(et: communications.CommunicationsModel) = new EnhancedCommunicationElements.EnhancedCommunicationsModel(et)
  implicit def enhanceCommsControlType(et: communications.ControlType) = new EnhancedCommunicationElements.EnhancedControlType(et)
  implicit def enhanceCommsProtocol(et: communications.Protocol) = new EnhancedCommunicationElements.EnhancedProtocol(et)

  implicit def enhanceCommonInfo(et: common.Info) = new EnhancedInfoElements.EnhancedInfo(et)
  implicit def enhanceCommonAttribute(et: common.Attribute) = new EnhancedInfoElements.EnhancedAttribute(et)
}