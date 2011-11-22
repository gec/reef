/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.benchmarks.output

class TeamCityStatisticsXml(fileName: String) {

  val outputStream = new java.io.FileOutputStream(fileName)
  var printStream = new java.io.PrintStream(outputStream)

  printStream.println("<build>")

  def close() = {
    printStream.println("</build>")
    printStream.close
  }

  def addRow(label: String, value: Any) {

    val safeLabel = label.replaceAll("[,.^&*(_]", "")

    printStream.println("\t<statisticsValue key=\"%s\" value=\"%s\" />".format(safeLabel, value))
  }
}