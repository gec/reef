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
import org.totalgrid.reef.client.service.proto.Calculations._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

object CalculationView {
  def printTable(calcs: List[Calculation]) = {
    Table.printTable(header, calcs.map(row(_)))
  }

  def header = {
    "OutputPoint" :: "Endpoint" :: "Formula" :: "Inputs" :: Nil
  }

  def row(a: Calculation) = {
    a.outputPoint.name.getOrElse("unknown") ::
      a.outputPoint.endpoint.name.getOrElse("unknown") ::
      a.getFormula.toString ::
      a.calcInputs.map { _.map { i => i.point.name.getOrElse("-") + "(" + i.variableName.getOrElse("-") + ")" }.mkString(",") }.getOrElse("-") ::
      Nil
  }

  def triggeringString(trigger: TriggerStrategy) = {
    trigger match {
      case t if (t.hasPeriodMs) => "Periodic(%d)".format(t.getPeriodMs)
      case t if (t.hasSchedule) => "Scheduled(%s)".format(t.getSchedule)
      case t if (t.hasUpdateAny) => "AnyUpdate"
      case t if (t.getVariablesCount > 0) => "TODO"
    }
  }

  def printInspect(a: Calculation) = {
    val lines =
      ("OutputPoint" :: a.outputPoint.name.getOrElse("unknown") :: Nil) ::
        ("Endpoint" :: a.outputPoint.endpoint.name.getOrElse("unknown") :: Nil) ::
        ("Formula" :: a.getFormula :: Nil) ::
        ("Triggering" :: a.triggering.map { triggeringString(_) }.getOrElse("unknown") :: Nil) ::
        Nil

    Table.justifyColumns(lines).foreach(line => println(line.mkString(" | ")))
  }
}