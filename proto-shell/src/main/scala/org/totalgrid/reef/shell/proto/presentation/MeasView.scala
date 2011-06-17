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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Measurements.{ Measurement, Quality }

object MeasView {

  def valueAndType(m: Measurement): (Any, String) = {
    if (m.getType == Measurement.Type.BOOL) {
      val repr = if (m.getBoolVal) "HIGH" else "LOW"
      (repr, "Binary")
    } else if (m.getType == Measurement.Type.DOUBLE) {
      (String.format("%.3f", m.getDoubleVal.asInstanceOf[AnyRef]), "Analog")
    } else if (m.getType == Measurement.Type.INT) {
      (m.getIntVal.toString, "Analog")
    } else if (m.getType == Measurement.Type.STRING) {
      (m.getStringVal, "String")
    } else {
      ("(unknown)", "(unknown)")
    }
  }

  def value(m: Measurement): Any = {
    val (value, typ) = valueAndType(m)
    value
  }

  def unit(m: Measurement) = if (m.hasUnit) m.getUnit else ""

  def shortQuality(m: Measurement) = {
    val q = m.getQuality

    if (q.getSource == Quality.Source.SUBSTITUTED) {
      "R"
    } else if (q.getOperatorBlocked) {
      "N"
    } else if (q.getTest) {
      "T"
    } else if (q.getDetailQual.getOldData) {
      "O"
    } else if (q.getValidity == Quality.Validity.QUESTIONABLE) {
      "A"
    } else if (q.getValidity != Quality.Validity.GOOD) {
      "B"
    } else {
      ""
    }
  }

  def longQuality(m: Measurement): String = {
    val q = m.getQuality
    longQuality(q)
  }

  def longQuality(q: Quality): String = {
    val dq = q.getDetailQual

    var list = List.empty[String]
    if (q.getOperatorBlocked) list ::= "NIS"
    if (q.getSource == Quality.Source.SUBSTITUTED) list ::= "replaced"
    if (q.getTest) list ::= "test"
    if (dq.getOverflow) list ::= "overflow"
    if (dq.getOutOfRange) list ::= "out of range"
    if (dq.getBadReference) list ::= "bad reference"
    if (dq.getOscillatory) list ::= "oscillatory"
    if (dq.getFailure) list ::= "failure"
    if (dq.getOldData) list ::= "old"
    if (dq.getInconsistent) list ::= "inconsistent"
    if (dq.getInaccurate) list ::= "inaccurate"

    val overall = q.getValidity match {
      case Quality.Validity.GOOD => "Good"
      case Quality.Validity.INVALID => "Invalid"
      case Quality.Validity.QUESTIONABLE => "Questionable"
    }

    overall + " (" + list.reverse.mkString("; ") + ")"
  }

  def timeString(m: Measurement): String = {
    new java.util.Date(m.getTime).toString
  }

  def header = {
    "Name" :: "Value" :: "Type" :: "Unit" :: "Q" :: "Time" :: Nil
  }
  def row(m: Measurement): List[String] = {
    val (value, typ) = valueAndType(m)
    m.getName :: value.toString :: typ :: unit(m) :: shortQuality(m) :: timeString(m) :: Nil
  }

  def printTable(list: List[Measurement]) = {
    Table.printTable(header, list.sortBy(_.getName).map(row(_)))
  }

  /**
   * Print all measurements that match the specified Quality fields. If two or
   * more quality fields are set, both have to match for the measurement to be
   * printed.
   */
  def printTableFilteredByQuality(measurements: List[Measurement], quality: Quality) = {
    val filtered = measurements.filter(m => {
      val q = m.getQuality

      // Each of these are true if we're NOT looking for that property or it's a match
      val validity = (!quality.hasValidity || quality.getValidity == q.getValidity)
      val source = (!quality.hasSource || quality.getSource == q.getSource)
      val blocked = (!quality.hasOperatorBlocked || quality.getOperatorBlocked == q.getOperatorBlocked)
      //TODO: Add filters for DetailQual bits.

      // If one didn't match, return false.
      validity && source && blocked
    })

    Table.printTable(header, filtered.sortBy(_.getName).map(row(_)))
  }

  def printInspect(m: Measurement) = {
    val (value, typ) = valueAndType(m)
    val lines =
      ("Name" :: m.getName :: Nil) ::
        ("Value" :: value.toString :: Nil) ::
        ("Type" :: typ :: Nil) ::
        ("Unit" :: unit(m) :: Nil) ::
        ("Quality" :: longQuality(m) :: Nil) ::
        ("Time" :: timeString(m) :: Nil) :: Nil

    val justLines = Table.justifyColumns(lines)
    Table.justifyColumns(lines).foreach(line => println(line.mkString(" | ")))
  }
}