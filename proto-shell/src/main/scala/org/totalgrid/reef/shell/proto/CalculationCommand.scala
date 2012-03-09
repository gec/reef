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
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Option => GogoOption, Argument, Command }

import scala.collection.JavaConversions._
import org.totalgrid.reef.shell.proto.presentation.CalculationView
import org.totalgrid.reef.client.service.entity.EntityRelation

@Command(scope = "calculation", name = "list", description = "List all calculations")
class CalculationListCommand extends ReefCommandSupport {

  def doCommand() = {
    CalculationView.printTable(services.getCalculations().toList)
  }
}

@Command(scope = "calculation", name = "view", description = "View a calculations details including its current values.\nExamples:\n" +
  "Show just the calculationd details:\n\tcalculation:view --no-values System.AverageStoredEnergy\n" +
  "Show and subscribe to only the calculation output:\n\tcalculation:view -w -o System.AverageStoredEnergy\n" +
  "Show the calculation and only the immedate input points:\n\tcalculation:view --no-indirect-parentsn System.AverageStoredEnergy\n" +
  "Show the calculation and all of its related points:\n\tcalculation:view --show-children System.AverageStoredEnergy")
class CalculationViewCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Name of the output point associated with the calculation", required = true, multiValued = false)
  var outputPointName: String = null

  @GogoOption(name = "--no-values", description = "Display current values of inputs and calculation", required = false, multiValued = false)
  var dontShowValues: Boolean = false

  @GogoOption(name = "--no-indirect-parents", description = "Dont show values for indirect parents", required = false, multiValued = false)
  var noIndirectParents: Boolean = false

  @GogoOption(name = "--no-direct-parents", description = "Dont show values for direct parents", required = false, multiValued = false)
  var noDirectParents: Boolean = false

  @GogoOption(name = "-o", description = "Show only calculation output. Equivilant to \"--no-indirect-parents --no-direct-parents\"", required = false, multiValued = false)
  var outputOnly: Boolean = false

  @GogoOption(name = "--show-children", description = "Don't show the values of child points", required = false, multiValued = false)
  var showChildren: Boolean = false

  @GogoOption(name = "-w", description = "Subscribe to measurement updates and continuously display new values coming in. Press cntrl-c to stop", required = false, multiValued = false)
  var watch: Boolean = false

  def doCommand() = {

    if (outputOnly) {
      noDirectParents = true
      noIndirectParents = true
    }

    val calc = services.getCalculationForPointByName(outputPointName)

    CalculationView.printInspect(calc)
    Console.out.println()

    if (!dontShowValues) {
      val directParentsNames = calc.getCalcInputsList.toList.map { _.getPoint.getName }

      def getRelatedPointNames(child: Boolean) = {
        services.getEntityRelations(calc.getOutputPoint.getUuid, List(new EntityRelation("calcs", List("Point"), child))).toList.map { _.getName }
      }

      val dependsOn = if (noIndirectParents) Nil else getRelatedPointNames(false).diff(directParentsNames)
      val isDependedOn = if (!showChildren) Nil else getRelatedPointNames(true)
      val directParents = if (noDirectParents) Nil else directParentsNames

      // TODO: make calculation distance numbers instead of symbols
      val pointNamesAndDistances: List[(String, String)] =
        dependsOn.diff(directParents).map { (_, "--") } :::
          directParents.map { (_, "-") } :::
          List(outputPointName).map { (_, "!") } :::
          isDependedOn.map { (_, "+") }

      val pointNames = pointNamesAndDistances.map { _._1 }

      if (!watch) {
        val meas = services.getMeasurementsByNames(pointNames).toList
        CalculationView.printMeasTable(pointNamesAndDistances.map { _._2 }.zip(meas))
      } else {
        val subResult = services.subscribeToMeasurementsByNames(pointNames)
        val widths = CalculationView.printMeasTable(pointNamesAndDistances.map { _._2 }.zip(subResult.getResult))
        runSubscription(subResult.getSubscription) { event =>
          val meas = event.getValue
          val distance = pointNamesAndDistances.find(_._1 == meas.getName).map { _._2 }.getOrElse("?")
          CalculationView.printMeasRow(distance, meas, widths)
        }
      }
    }
  }

  // calculation:view System.AverageStoredEnergy
  // calculation:view Microgrid1.Input.Energy
  // calculation:view -w Microgrid1.Input.Power
  // calculation:view --no-indirect-parents --no-direct-parents -w Microgrid1.Input.Power
}
