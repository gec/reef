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
package org.totalgrid.reef.services.core

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.{ ApplicationSchema, Entity, EntityEdge => Edge, EntityDerivedEdge => Derived }
import org.totalgrid.reef.services.SilentRequestContext

object ModelSeed {

  val edgeModel = new EntityEdgeServiceModel
  val context = new SilentRequestContext

  def seed() {
    if (ApplicationSchema.entities.Count.head == 0) {
      pittsboro.seed()
      apex.seed()
    }
  }

  case class Node(typ: String, name: String, subNodes: Node*) {
    def seed(ancestors: List[Entity] = Nil, names: List[String] = Nil): Unit = {
      val nameList = name :: names
      val fullName = nameList.reverse.mkString(".")
      val me = EntityTestSeed.addEntity(fullName, typ)
      if (ancestors.length > 0) edgeModel.addEdge(context, ancestors.head, me, "owns")
      subNodes.foreach(node => node.seed(me :: ancestors, nameList))
    }
  }

  def bus(name: String) = Node("Bus", name, Node("Point", "Kv"))
  def busWithFreq(name: String) = Node("Bus", name, Node("Point", "Kv"), Node("Point", "Freq"))

  def line(name: String) =
    Node("Line", name,
      Node("Point", "Mw"),
      Node("Point", "Mvar"),
      Node("Point", "Mva"))

  def lineWithCurrent(name: String) =
    Node("Line", name,
      Node("Point", "Mw"),
      Node("Point", "Mvar"),
      Node("Point", "Ia"),
      Node("Point", "Ib"),
      Node("Point", "Ic"))

  def generator(name: String) =
    Node("Generator", name,
      Node("Point", "Mw"),
      Node("Point", "Mvar"),
      Node("Point", "Mwh"))

  def status(name: String) = Node("StatusHolder", name, Node("Point", "STTS"))

  def battery(name: String) = Node("Battery", name, Node("Point", "V"))

  def fullBreaker(name: String) =
    Node("Breaker", name,
      Node("Point", "Bkr",
        Node("Command", "Trip"),
        Node("Command", "Close")),
      Node("Point", "APh_Trip"),
      Node("Point", "BPh_Trip"),
      Node("Point", "Auto_Man"),
      Node("Point", "CPh_Trip"),
      Node("Point", "50_Trip"),
      Node("Point", "51_Trip"),
      Node("Point", "79_LO"),
      Node("Point", "G_Trip"))

  def simpleBreaker(name: String) =
    Node("Breaker", name,
      Node("Point", "Bkr",
        Node("Command", "Trip"),
        Node("Command", "Close")))

  def switch(name: String) =
    Node("Switch", name,
      Node("Point", "Stts",
        Node("Command", "Open"),
        Node("Command", "Close")))

  def pittsboro =
    Node("Substation", "Pittsboro",
      simpleBreaker("2401"),
      simpleBreaker("2402"),
      simpleBreaker("2403"),
      simpleBreaker("124"),
      simpleBreaker("Btie"),
      simpleBreaker("1201"),
      simpleBreaker("1202"),
      line("L241"),
      line("L242"),
      line("L243"),
      line("F121"),
      line("F122"),
      bus("B12"),
      bus("B24"),
      status("STATN SUPV_OFF"),
      status("STATN INVERTER"),
      status("STATN GPS_CLOCK"),
      battery("STATN 125_BATTERY1"),
      battery("STATN 125_BATTERY2"),
      status("STATN DOOR_ALARM"),
      switch("DS24011"),
      switch("DS24012"),
      switch("DS24021"),
      switch("DS24022"),
      switch("DS24031"),
      switch("DS24032"),
      switch("DSTie1"),
      switch("DSTie2"),
      switch("DS12011"),
      switch("DS12012"),
      switch("DS12021"),
      switch("DS12022"))

  def apex =
    Node("Substation", "Apex",
      busWithFreq("Bus1"),
      lineWithCurrent("L241"),
      lineWithCurrent("L242"),
      generator("G1"),
      lineWithCurrent("155"),
      status("STATN SUPV_OFF"),
      status("STATN INVERTER"),
      status("STATN GPS_CLOCK"),
      battery("STATN 125_BATTERY1"),
      battery("STATN 125_BATTERY2"),
      status("STATN DOOR_ALARM"),
      fullBreaker("CB11001"),
      fullBreaker("CB11002"),
      fullBreaker("CB11003"),
      fullBreaker("CB11004"),
      status("STATN FIRE_ALARM"))

}