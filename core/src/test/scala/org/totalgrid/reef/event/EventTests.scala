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
package org.totalgrid.reef.event

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

// Important! Rename java.lang.System or fully qualify events.
import java.lang.{ System => JavaSystem }

@RunWith(classOf[JUnitRunner])
class EventTests extends FunSuite with ShouldMatchers {
  import EventType._

  def castToString(s: String) = s
  def castToEventType(e: EventType) = e

  test("EventType.toString()") {

    Scada.ControlExe.toString() should equal("Scada.ControlExe")
    System.UserLogin.toString() should equal("System.UserLogin")
    System.UserLogout.toString() should equal("System.UserLogout")
    System.SubsystemStarting.toString() should equal("System.SubsystemStarting")
    System.SubsystemStarted.toString() should equal("System.SubsystemStarted")
    System.SubsystemStopping.toString() should equal("System.SubsystemStopping")
    System.SubsystemStopped.toString() should equal("System.SubsystemStopped")
  }

  test("Implicit EventType to String") {

    castToString(Scada.ControlExe) should equal("Scada.ControlExe")
    castToString(System.UserLogin) should equal("System.UserLogin")
    castToString(System.UserLogout) should equal("System.UserLogout")
    castToString(System.SubsystemStarting) should equal("System.SubsystemStarting")
    castToString(System.SubsystemStarted) should equal("System.SubsystemStarted")
    castToString(System.SubsystemStopping) should equal("System.SubsystemStopping")
    castToString(System.SubsystemStopped) should equal("System.SubsystemStopped")
  }

  test("Implicit String to EventType") {

    castToEventType("Scada.ControlExe") should equal(Scada.ControlExe)
    castToEventType("System.UserLogin") should equal(System.UserLogin)
    castToEventType("System.UserLogout") should equal(System.UserLogout)
    castToEventType("System.SubsystemStarting") should equal(System.SubsystemStarting)
    castToEventType("System.SubsystemStarted") should equal(System.SubsystemStarted)
    castToEventType("System.SubsystemStopping") should equal(System.SubsystemStopping)
    castToEventType("System.SubsystemStopped") should equal(System.SubsystemStopped)
  }

}
