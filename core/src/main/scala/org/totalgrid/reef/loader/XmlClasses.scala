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

import org.totalgrid.reef.loader.configuration._

class XmlMessage(
    _name: String,
    _type: String = "",
    _severity: Int = 0,
    _value: String = "",
    _state: String = "") extends Message {

  setName(_name)
  if (_type.length > 0)
    setType(_type)
  if (_severity > 0)
    setSeverity(_severity)
  if (_value.length > 0)
    setValue(_value)
  if (_state.length > 0)
    setState(_state)
}

class XmlMessageSet(
    _name: String,
    _type: String = "",
    _severity: Int = 0,
    _state: String = "") extends MessageSet {

  setName(_name)
  if (_type.length > 0)
    setType(_type)
  if (_severity > 0)
    setSeverity(_severity)
  if (_state.length > 0)
    setState(_state)
}

class XmlRange(
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

class XmlControl(_name: String) extends equipment.Control {
  setName(_name)
}
class XmlType(_name: String) extends equipment.Type {
  setName(_name)
}

class XmlUnexpected(_actionSet: String) extends equipment.Unexpected {
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

trait XmlPointType { self: equipment.PointType =>
  def init(_name: String, _unit: String = "", _pointProfile: String = "") = {
    setName(_name)
    if (_unit.length > 0)
      setUnit(_unit)
    if (_pointProfile.length > 0)
      setPointProfile(_pointProfile)
  }
  def add(x: XmlRange) = { getRange.add(x); this }
  def add(x: XmlControl) = { getControl.add(x); this }
  def add(x: XmlUnexpected) = { getUnexpected.add(x); this }
  //def add( x: XmlValueMap*) = { getValueMap.add(x); this }
}
class XmlPointProfile(_name: String) extends equipment.PointProfile {
  setName(_name)
  def add(x: XmlRange) = { getRange.add(x); this }
  def add(x: XmlControl) = { getControl.add(x); this }
  def add(x: XmlUnexpected) = { getUnexpected.add(x); this }
  //def add( x: XmlValueMap*) = { getValueMap.add(x); this }
}
class XmlStatus(_name: String, _unit: String = "", _pointProfile: Option[XmlPointProfile] = None) extends equipment.Status with XmlPointType {
  init(_name, _unit, if (_pointProfile.isDefined) _pointProfile.get.getName else "")
}
class XmlAnalog(_name: String, _unit: String = "", _pointProfile: String = "") extends equipment.Analog with XmlPointType {
  init(_name, _unit, _pointProfile)
}
class XmlCounter(_name: String, _unit: String = "", _pointProfile: String = "") extends equipment.Counter with XmlPointType {
  init(_name, _unit, _pointProfile)
}

class XmlEquipmentProfile(_name: String) extends equipment.EquipmentProfile {
  setName(_name)
  def add(x: org.totalgrid.reef.loader.equipment.EquipmentProfile) = { getEquipmentProfile.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Type) = { getType.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Control) = { getControl.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Status) = { getStatus.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Analog) = { getAnalog.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Counter) = { getCounter.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Equipment) = { getEquipment.add(x); this }
}
class XmlEquipment(_name: String) extends equipment.Equipment {
  setName(_name)
  def add(x: org.totalgrid.reef.loader.equipment.EquipmentProfile) = { getEquipmentProfile.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Type) = { getType.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Control) = { getControl.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Status) = { getStatus.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Analog) = { getAnalog.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Counter) = { getCounter.add(x); this }
  def add(x: org.totalgrid.reef.loader.equipment.Equipment) = { getEquipment.add(x); this }
}

class XmlProfiles extends equipment.Profiles {
  def add(p: XmlPointProfile) = { getPointProfile.add(p); this }
  def add(p: XmlEquipmentProfile) = { getEquipmentProfile.add(p); this }
}
class XmlEquipmentModel extends equipment.EquipmentModel {
  def add(e: org.totalgrid.reef.loader.equipment.Equipment) = { getEquipment.add(e); this }
  def reset: Unit = {
    setProfiles(new XmlProfiles)
    getEquipment.clear
  }
}

class XmlActionSet(_name: String) extends ActionSet {
  setName(_name)
}

class XmlActionModel extends ActionModel {
  def add(x: XmlActionSet) = { getActionSet.add(x); this }
  def reset: Unit = {
    getActionSet.clear
  }

  def addNominal(name: String): XmlActionModel = {
    val set = new XmlActionSet(name)
    set.getRising.getMessage.add(new XmlMessage("Scada.OutOfNominal"))
    set.getHigh.setSetAbnormal(new Object)
    getActionSet.add(set)
    this
  }
}