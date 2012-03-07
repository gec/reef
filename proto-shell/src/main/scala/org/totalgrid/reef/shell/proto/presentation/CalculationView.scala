package org.totalgrid.reef.shell.proto.presentation

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

import org.totalgrid.reef.client.service.proto.OptionalProtos._

import org.totalgrid.reef.util.Table
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Calculations._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

object CalculationView {
  def printTable(calcs: List[Calculation]) = {
    Table.printTable(header, calcs.map(row(_)))
  }

  def header = {
    "OutputPoint" :: "Formula" :: "Unit" :: Nil
  }

  def row(a: Calculation) = {
    a.outputPoint.name.getOrElse("unknown") ::
      a.getFormula.toString ::
      a.outputPoint.unit.getOrElse("unknown") ::
      Nil
  }

  def triggeringString(trigger: TriggerStrategy) = {
    trigger match {
      case t if (t.hasPeriodMs) => "Periodic(%d)".format(t.getPeriodMs)
      case t if (t.hasSchedule) => "Scheduled(%s)".format(t.getSchedule)
      case t if (t.hasUpdateAny) => "AnyUpdate"
      /*case t if (t.getVariablesCount > 0) =>
        t.getVariablesList.toList.map { _.getVariableName }.mkString("Update(", ",", ")") */
    }
  }

  def timeString(time: Long) = {
    new java.util.Date(time).toString
  }

  def rangeString(calcInput: CalculationInput): String = {
    calcInput match {
      case i if (i.hasSingle) => "SINGLE(%s)".format(i.getSingle.getStrategy)
      case i if (i.hasRange) =>
        i.getRange match {
          case r if (r.hasLimit && !r.hasFromMs && !r.hasToMs) => "RECENT_SAMPLES(%d)".format(r.getLimit)
          case r if (r.hasFromMs && !r.hasToMs) => "RECENT_HISTORY(%d)".format(r.getFromMs)
          case r if (r.hasFromMs && r.hasToMs) => "FIXED_TIME(%s,%s)".format(timeString(r.getFromMs), timeString(r.getToMs))
          case _ => "UNKNOWN(%s)".format(i.getRange)
        }
      case _ => "UNKNOWN(%s)".format(calcInput)
    }
  }

  def printInspect(a: Calculation) = {

    val inputLines: List[List[String]] = a.calcInputs.map { jargs =>
      val args = jargs.toList
      ("Var" :: "Point" :: "Type" :: Nil) :: args.map { optInput =>
        val input = optInput.get
        input.getVariableName :: input.getPoint.getName :: rangeString(input) :: Nil
      }
    }.getOrElse(Nil)

    val lines: List[List[String]] =
      (("OutputPoint" :: a.outputPoint.name.getOrElse("unknown") :: a.outputPoint.unit.getOrElse("unknown") :: Nil) ::
        ("Endpoint" :: a.outputPoint.endpoint.name.getOrElse("unknown") :: Nil) ::
        ("Triggering" :: a.triggering.map { triggeringString(_) }.getOrElse("unknown") :: Nil) :: Nil) :::
        inputLines :::
        (("Formula" :: a.getFormula :: Nil) :: Nil)

    Table.renderRows(lines, " | ")
  }

  def printMeasTable(meases: List[(String, Measurement)]) = {
    Table.printTable(List("Dist") ::: MeasView.header, meases.map { case (d, m) => getMeasRow(d, m) })
  }

  def getMeasRow(distance: String, m: Measurement) = {
    List(distance) ::: MeasView.row(m)
  }

  def printMeasRow(distance: String, meas: Measurement, widths: List[Int]) {
    Table.renderTableRow(CalculationView.getMeasRow(distance, meas), widths)
  }
}