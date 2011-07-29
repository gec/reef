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
package org.totalgrid.reef.loader.sx.equipment

import org.totalgrid.reef.loader.equipment // jaxb java classes

class Range(
    _actionSet: String,
    _low: Int,
    _high: Int,
    _deadband: Int = 0,
    _state: String = "") extends equipment.Range {

  setActionSet(_actionSet)
  setLow(_low)
  setHigh(_high)
  if (_deadband > 0)
    setDeadband(_deadband)
}

class Control(_name: String) extends equipment.Control {
  setName(_name)
}
class Type(_name: String) extends equipment.Type {
  setName(_name)
}

class Unexpected private (_actionSet: String) extends equipment.Unexpected {
  setActionSet(_actionSet)

  def this(_value: String, _actionSet: String) = {
    this(_actionSet)
    setStringValue(_value)
  }
  def this(_value: Boolean, _actionSet: String) = {
    this(_actionSet)
    setBooleanValue(_value)
  }
  def this(_value: Int, _actionSet: String) = {
    this(_actionSet)
    setIntValue(_value)
  }
  def this(_value: Double, _actionSet: String) = {
    this(_actionSet)
    setDoubleValue(_value)
  }
}

class ValueMap(fromString: String, toString: String) extends equipment.ValueMap {
  setFromValue(fromString)
  setToValue(toString)
}

class Transform(fromUnit: String, toUnit: String, valueMaps: ValueMap*) extends equipment.Transform {
  setFromUnit(fromUnit)
  setToUnit(toUnit)
  setTransformationType(equipment.TransformType.STATUS)
  valueMaps.foreach(getValueMap.add(_))
}

trait PointType[A] { self: equipment.PointType =>
  def init(_name: String, _unit: String = "", _pointProfile: String = "") = {
    setName(_name)
    if (_unit.length > 0)
      setUnit(_unit)
    if (_pointProfile.length > 0)
      setPointProfile(_pointProfile)
  }
  def add(x: Range): A = { getTransformOrControlOrSetpoint.add(x); this.asInstanceOf[A] }
  def add(x: Control): A = { getTransformOrControlOrSetpoint.add(x); this.asInstanceOf[A] }
  def add(x: Unexpected): A = { getTransformOrControlOrSetpoint.add(x); this.asInstanceOf[A] }
  def add(x: Transform): A = { getTransformOrControlOrSetpoint.add(x); this.asInstanceOf[A] }
}
class PointProfile(_name: String) extends equipment.PointProfile {
  setName(_name)
  def add(x: Range) = { getTransformOrControlOrSetpoint.add(x); this }
  def add(x: Control) = { getTransformOrControlOrSetpoint.add(x); this }
  def add(x: Unexpected) = { getTransformOrControlOrSetpoint.add(x); this }
  def add(x: Transform) = { getTransformOrControlOrSetpoint.add(x); this }
}
class Status(_name: String, _unit: String = "", _pointProfile: Option[PointProfile] = None) extends equipment.Status with PointType[Status] {
  init(_name, _unit, if (_pointProfile.isDefined) _pointProfile.get.getName else "")
}
class Analog(_name: String, _unit: String = "", _pointProfile: String = "") extends equipment.Analog with PointType[Analog] {
  init(_name, _unit, _pointProfile)
}
class Counter(_name: String, _unit: String = "", _pointProfile: String = "") extends equipment.Counter with PointType[Counter] {
  init(_name, _unit, _pointProfile)
}

trait EquipmentType[A] { self: equipment.EquipmentType =>
  def add(x: org.totalgrid.reef.loader.equipment.EquipmentProfile) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Type) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Control) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Status) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Analog) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Counter) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
  def add(x: org.totalgrid.reef.loader.equipment.Equipment) = { getEquipmentProfileOrTypeOrControl.add(x); this.asInstanceOf[A] }
}

class EquipmentProfile(_name: String) extends equipment.EquipmentProfile with EquipmentType[EquipmentProfile] {
  setName(_name)
}

class Equipment(_name: String) extends equipment.Equipment with EquipmentType[Equipment] {
  setName(_name)
}

class Profiles extends equipment.Profiles {
  def add(p: PointProfile) = { getPointProfile.add(p); this }
  def add(p: EquipmentProfile) = { getEquipmentProfile.add(p); this }
}
class EquipmentModel extends equipment.EquipmentModel {
  def add(e: org.totalgrid.reef.loader.equipment.Equipment) = { getEquipment.add(e); this }
  def reset: Unit = {
    setProfiles(new Profiles)
    getEquipment.clear
  }
}

