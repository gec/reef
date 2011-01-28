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
package org.totalgrid.reef.shell.proto.presentation

object Table {

  def normalizeNumCols(rows: List[List[String]]) = {
    val max = rows.map(_.length).max
    rows.map(row => row.padTo(max, ""))
  }

  def justifyColumns(rows: List[List[String]]) = {
    val widths = rows.foldLeft(List.empty[Int]) {
      case (widths, row) =>
        val w = if (widths.length < row.length) widths.padTo(row.length, 0) else widths
        row.map(_.length).zip(w).map { case (a, b) => if (a > b) a else b }
    }

    rows.map { row =>
      row.zip(widths).map {
        case (str, width) =>
          str.padTo(width, " ").mkString
      }
    }
  }

  def rowLength(line: List[String]) = {
    line.foldLeft(0)(_ + _.length)
  }

  def printTable(header: List[String], rows: List[List[String]]) = {
    val just = justifyColumns(header :: rows)
    val headStr = just.head.mkString("     ")
    println(headStr)
    println("".padTo(headStr.length, "-").mkString)
    just.tail.foreach(line => println(line.mkString("  |  ")))
  }

  def renderRows(rows: List[List[String]], sep: String = "") = {
    Table.justifyColumns(rows).foreach(line => println(line.mkString(sep)))
  }

}