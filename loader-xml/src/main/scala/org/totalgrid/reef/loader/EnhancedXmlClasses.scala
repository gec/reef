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
  }

  class EnhancedPointType(pt: PointType) {

    lazy val parts = pt.getTransformOrControlOrSetpoint.toList

    def getType = withClass(parts, classOf[Type])
    def getRange = withClass(parts, classOf[Range])
    def getUnexpected = withClass(parts, classOf[Unexpected])
    def getControl = withClass(parts, classOf[Control])
    def getTransform = withClass(parts, classOf[Transform])
    def getSetpoint = withClass(parts, classOf[Setpoint])
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
  }

  class EnhancedCommunicationsModel(cm: CommunicationsModel) {
    lazy val parts = cm.getProfilesOrInterfaceOrEndpoint.toList

    def getProfiles = withClass(parts, classOf[Profiles])
    def getEndpoint = withClass(parts, classOf[Endpoint])
    def getInterface = withClass(parts, classOf[Interface])

    //    def getInterface = withClass(parts, classOf[Interface]) match{
    //      case List(one) => Some(one)
    //      case Nil => None
    //      case _ => throw new LoadingException("More than one interface defined, should be 0 or 1")
    //    }
  }

  //  class EnhancedPointProfile(pt: PointProfile) {
  //
  //    lazy val parts = pt.get.toList
  //
  //    def getPointProfile = withClass(parts, classOf[PointProfile])
  //    def getControlProfile = withClass(parts, classOf[ControlProfile])
  //    def getEquipmentProfile = withClass(parts, classOf[EquipmentProfile])
  //    def getEndpointProfile = withClass(parts, classOf[EndpointProfile])
  //  }

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

object EnhancedXmlClasses {
  implicit def enhanceEquEquipment(et: equipment.EquipmentType) = new EnhancedEquipmentElements.EnhancedEquipment(et)
  implicit def enhanceEquPointType(et: equipment.PointType) = new EnhancedEquipmentElements.EnhancedPointType(et)
  implicit def enhanceEquProfiles(et: equipment.Profiles) = new EnhancedEquipmentElements.EnhancedProfiles(et)

  implicit def enhanceCommsEquipment(et: communications.EquipmentType) = new EnhancedCommunicationElements.EnhancedEquipment(et)
  implicit def enhanceCommsProfiles(et: communications.Profiles) = new EnhancedCommunicationElements.EnhancedProfiles(et)
  implicit def enhanceCommsEndpointType(et: communications.EndpointType) = new EnhancedCommunicationElements.EnhancedEndpointType(et)
  implicit def enhanceCommsCommunicationModel(et: communications.CommunicationsModel) = new EnhancedCommunicationElements.EnhancedCommunicationsModel(et)
  implicit def enhanceCommsControlType(et: communications.ControlType) = new EnhancedCommunicationElements.EnhancedControlType(et)
  implicit def enhanceCommsProtocol(et: communications.Protocol) = new EnhancedCommunicationElements.EnhancedProtocol(et)
  //  implicit def enhanceCommsPointProfile(et: communications.PointProfile) = new EnhancedCommunicationElements.EnhancedPointProfile(et)
}