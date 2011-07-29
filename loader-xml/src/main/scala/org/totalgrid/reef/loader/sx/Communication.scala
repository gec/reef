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
package org.totalgrid.reef.loader.sx.communications

import org.totalgrid.reef.loader.communications
import org.totalgrid.reef.loader.sx.ConfigFile

// jaxb java classes

class Type(_name: String) extends communications.Type {
  setName(_name)
}

class Protocol(_name: String, _configFileName: Option[String]) extends communications.Protocol {
  setName(_name)
  if (_configFileName.isDefined)
    add(new ConfigFile(_configFileName.get))

  def add(x: ConfigFile) = { getConfigFile.add(x); this }
}

class SimOptions(_value: Int) extends communications.SimOptions {
  setDelay(_value)
}

class OptionsDnp3(_type: String, _onTime: Int, _offTime: Int, _count: Int) extends communications.OptionsDnp3 {
  setType(_type)
  setOnTime(_onTime)
  setOffTime(_offTime)
  setCount(_count)
}

class Scale() extends communications.Scale {
  def this(_rawLow: Double, _rawHigh: Double, _engLow: Double, _engHigh: Double, _engUnit: String) = {
    this()
    setRawLow(_rawLow)
    setRawHigh(_rawHigh)
    setEngLow(_engLow)
    setEngHigh(_engHigh)
    setEngUnit(_engUnit)
  }
  def this(_slope: Double, _offset: Double) = {
    this()
    setSlope(_slope)
    setOffset(_offset)
  }
}

trait ControlType[A] { self: communications.ControlType =>
  def add(x: ControlProfile): A = { getControlProfile.add(x); this.asInstanceOf[A] }
  def set(x: OptionsDnp3): A = { setOptionsDnp3(x); this.asInstanceOf[A] }
}

class ControlProfile(_name: String) extends communications.ControlProfile with ControlType[ControlProfile] {
  setName(_name)
  def this(_name: String, _index: Option[Int]) = {
    this(_name)
    _index.foreach(setIndex)
  }
}

class Control(_name: String, _index: Option[Int]) extends communications.Control with ControlType[Control] {
  setName(_name)
  _index.foreach(setIndex)
  def this(_name: String, _index: Option[Int], _profile: ControlProfile) = {
    this(_name, _index)
    add(_profile)
  }
}

trait PointType[A] { self: communications.PointType =>
  def init(_name: String, _index: Option[Int], _unit: Option[String], _pointProfile: Option[PointProfile]) = {
    setName(_name)
    _index.foreach(setIndex)
    _unit.foreach(setUnit)
    _pointProfile.foreach(p => setPointProfile(p.getName))
  }
  def set(x: Scale): A = { setScale(x); this.asInstanceOf[A] }
}

class PointProfile(_name: String, _unit: Option[String] = None, _index: Option[Int] = None) extends communications.PointProfile with PointType[PointProfile] {
  setName(_name)
  _unit.foreach(setUnit)
  _index.foreach(setIndex)
  def this(_name: String, _unit: String) = this(_name, Some(_unit))
  def this(_name: String, _index: Option[Int]) = this(_name, None, _index)
}
class Status(_name: String, _index: Option[Int], _unit: Option[String], _pointProfile: Option[PointProfile] = None) extends communications.Status with PointType[Status] {
  init(_name, _index, _unit, _pointProfile)
  def this(_name: String, _index: Option[Int], _pointProfile: PointProfile) = this(_name, _index, None, Some(_pointProfile))
  def this(_name: String, _index: Option[Int], _unit: String) = this(_name, _index, Some(_unit))
  def this(_name: String, _index: Option[Int]) = this(_name, _index, None)
  def this(_name: String) = this(_name, None, None)
}
class Analog(_name: String, _index: Option[Int], _unit: Option[String], _pointProfile: Option[PointProfile] = None) extends communications.Analog with PointType[Analog] {
  init(_name, _index, _unit, _pointProfile)
  def this(_name: String, _index: Option[Int], _pointProfile: PointProfile) = this(_name, _index, None, Some(_pointProfile))
  def this(_name: String, _index: Option[Int], _unit: String) = this(_name, _index, Some(_unit))
  def this(_name: String, _index: Option[Int]) = this(_name, _index, None)
  def this(_name: String) = this(_name, None, None)
}
class Counter(_name: String, _index: Option[Int], _unit: Option[String], _pointProfile: Option[PointProfile] = None) extends communications.Counter with PointType[Counter] {
  init(_name, _index, _unit, _pointProfile)
  def this(_name: String, _index: Option[Int], _pointProfile: PointProfile) = this(_name, _index, None, Some(_pointProfile))
  def this(_name: String, _index: Option[Int], _unit: String) = this(_name, _index, Some(_unit))
  def this(_name: String, _index: Option[Int]) = this(_name, _index, None)
  def this(_name: String) = this(_name, None, None)
}

class Profiles extends communications.Profiles {
  def add(p: ControlProfile) = { getControlProfile.add(p); this }
  def add(p: PointProfile) = { getPointProfile.add(p); this }
  def add(p: EndpointProfile) = { getEndpointProfile.add(p); this }
  def add(p: EquipmentProfile) = { getEquipmentProfile.add(p); this }
}

trait EquipmentType[A] { self: communications.EquipmentType =>
  def add(x: EquipmentProfile) = { getEquipmentProfile.add(x); this.asInstanceOf[A] }
  def add(x: Control) = { getControl.add(x); this.asInstanceOf[A] }
  def add(x: Status) = { getStatus.add(x); this.asInstanceOf[A] }
  def add(x: Analog) = { getAnalog.add(x); this.asInstanceOf[A] }
  def add(x: Counter) = { getCounter.add(x); this.asInstanceOf[A] }
  def add(x: Equipment) = { getEquipment.add(x); this.asInstanceOf[A] }
}

class EquipmentProfile(_name: String) extends communications.EquipmentProfile with EquipmentType[EquipmentProfile] {
  setName(_name)
}

class Equipment(_name: String) extends communications.Equipment with EquipmentType[Equipment] {
  setName(_name)
}

trait EndpointType[A] { self: communications.EndpointType =>
  def init(_name: String, _protocolName: Option[String], _configFileName: Option[String]): Unit = {
    setName(_name)
    add(new Type("Endpoint"))
    if (_protocolName.isDefined) {
      setProtocol(new Protocol(_protocolName.get, _configFileName))
    }
  }
  def add(x: EndpointProfile) = { getEndpointProfile.add(x); this.asInstanceOf[A] }
  def add(x: Type) = { getType.add(x); this.asInstanceOf[A] }
  def add(x: Equipment) = { getEquipment.add(x); this.asInstanceOf[A] }
  def set(x: Interface) = { setInterface(x); this.asInstanceOf[A] }
}
class EndpointProfile(_name: String, _protocolName: Option[String] = Some("benchmark"), _configFileName: Option[String] = None) extends communications.EndpointProfile with EndpointType[EndpointProfile] {
  init(_name, _protocolName, _configFileName)
}
class Endpoint(_name: String, _protocolName: Option[String] = Some("benchmark"), _configFileName: Option[String] = None) extends communications.Endpoint with EndpointType[Endpoint] {
  init(_name, _protocolName, _configFileName)
}

class Interface(
    _name: String,
    _ip: Option[String],
    _port: Option[Int],
    _netmask: Option[String] = None,
    _network: Option[String] = None) extends communications.Interface {

  setName(_name)
  _ip.foreach(setIp)
  _port.foreach(setPort)
  _netmask.foreach(setNetmask)
  _network.foreach(setNetwork)

  def this(_name: String) = this(_name, None, None)
  def this(_name: String, _port: Int) = this(_name, None, Some(_port))
  def this(_name: String, _ip: String, _port: Int) = this(_name, Some(_ip), Some(_port))
  def this(_name: String, _ip: String, _port: Int, _netmask: String, _network: String) = this(_name, Some(_ip), Some(_port), Some(_netmask), Some(_network))
}

class CommunicationsModel extends communications.CommunicationsModel {
  def add(e: Interface) = { getInterface.add(e); this }
  def add(e: Endpoint) = { getEndpoint.add(e); this }
  def set(p: Profiles) = { setProfiles(p); this }
  def reset: Unit = {
    setProfiles(new Profiles)
    getInterface.clear
    getEndpoint.clear
  }
}

