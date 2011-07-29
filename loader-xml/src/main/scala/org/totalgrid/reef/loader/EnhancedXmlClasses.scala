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
  def withClass[T](list: List[Any], clazz: Class[T]): List[T] = {
    list.filter { clazz.isInstance(_) }.map { _.asInstanceOf[T] }
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
}

object EnhancedXmlClasses {
  implicit def enhanceEquipment(et: equipment.EquipmentType) = new EnhancedEquipmentElements.EnhancedEquipment(et)

  implicit def enhancePointType(et: equipment.PointType) = new EnhancedEquipmentElements.EnhancedPointType(et)

  implicit def enhanceCommsEquipment(et: communications.EquipmentType) = new EnhancedCommunicationElements.EnhancedEquipment(et)

  implicit def enhanceCommsProfiles(et: communications.Profiles) = new EnhancedCommunicationElements.EnhancedProfiles(et)
}