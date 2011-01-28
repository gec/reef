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
package org.totalgrid.reef.event

import scala.collection.mutable.HashMap

/*
 * All Events derive from EventType.
 * 
 * TODO: Need to extract a generic Enumeration class from this.
 */
abstract class EventType() {
  val name = getName

  /**
   *  The className org.psi.event.EventType$Scada$ControlExe$ is translated
   *  to Scada.ControlExe .
   */
  private def getName = {
    val className = this.getClass.getName
    val dollarIndex = className.indexOf('$') // $ is right after org.psi.event.EventType
    val theName = className.substring(dollarIndex + 1, className.length - 1).replace('$', '.')

    // Store in map for later lookup by name.
    EventType.nameMap += (theName -> this)

    theName
  }

  override def toString = name

}

object EventType {

  // Map container for all EventType's
  // TODO: Want to use this to check that localization resource strings exist for all EventType's.
  // TODO: The problem is that the resource files are by subsystem and we don't know subsystems here. 
  // TODO: Disconnect? Can each subsystem need a different UserLogin message? We probably
  // TODO: need a "system" resource file, then one for each subsystem. The subsystems are the
  // TODO: main objects below. Could the enums even be defined separately in each subsystem
  // TODO: and derive from EventType? This companion object is only allowed in one file and
  // TODO: can't be inherited - so FEP would have to have EventType's like FEP.SomeEvent (which
  // TODO: isn't bad.
  //
  private val nameMap = HashMap[String, EventType]()

  object Scada {
    case object ControlExe extends EventType
    case object OutOfNominal extends EventType
    case object OutOfReasonable extends EventType
  }

  object System {
    case object UserLogin extends EventType
    case object UserLogout extends EventType
    case object SubsystemStarting extends EventType
    case object SubsystemStarted extends EventType
    case object SubsystemStopping extends EventType
    case object SubsystemStopped extends EventType
  }

  implicit def eventTypeToString(e: EventType) = e.toString
  implicit def stringToEventType(s: String) = nameMap(s)

  def fromString(s: String): Option[EventType] = nameMap.get(s)
}

