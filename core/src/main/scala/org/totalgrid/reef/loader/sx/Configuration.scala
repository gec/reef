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
package org.totalgrid.reef.loader.sx

import org.totalgrid.reef.loader.configuration // jaxb java classes

class Message(
    _name: String,
    _type: String = "",
    _severity: Int = 0,
    _value: String = "",
    _state: String = "") extends configuration.Message {

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

class MessageSet(
    _name: String,
    _type: String = "",
    _severity: Int = 0,
    _state: String = "") extends configuration.MessageSet {

  setName(_name)
  if (_type.length > 0)
    setType(_type)
  if (_severity > 0)
    setSeverity(_severity)
  if (_state.length > 0)
    setState(_state)

  def add(x: MessageSet) = { getMessageSet.add(x); this }
  def add(x: Message) = { getMessage.add(x); this }
  def reset: Unit = {
    getMessageSet.clear
    getMessage.clear
  }
}

class MessageModel extends configuration.MessageModel {
  def add(x: MessageSet) = { getMessageSet.add(x); this }
  def add(x: Message) = { getMessage.add(x); this }
  def reset: Unit = {
    getMessageSet.clear
    getMessage.clear
  }

}

class ActionSet(_name: String) extends configuration.ActionSet {
  setName(_name)
}

class ActionModel extends configuration.ActionModel {
  def add(x: ActionSet) = { getActionSet.add(x); this }
  def reset: Unit = {
    getActionSet.clear
  }

  def addNominal(name: String): ActionModel = {
    val set = new ActionSet(name)
    set.getRising.getMessage.add(new Message("Scada.OutOfNominal"))
    set.getHigh.setSetAbnormal(new Object)
    getActionSet.add(set)
    this
  }
}

class Configuration(_version: String) extends configuration.Configuration {
  setVersion(_version)
}