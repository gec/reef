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

import org.totalgrid.reef.util.Table
import org.totalgrid.reef.client.RequestHeaders
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders

object ClientHeadersView {

  private def headerRow = "Field" :: "Value" :: Nil

  private def getRows(headers: BasicRequestHeaders) = {
    List("AuthToken", "xxxxxxxxxxxx") ::
      List("Timeout", headers.getTimeout.map { _.toString }.getOrElse("default")) ::
      List("ResultLimit", headers.getResultLimit().map { _.toString }.getOrElse("default")) :: Nil
  }

  def displayHeaders(headers: RequestHeaders) = {
    Table.printTable(headerRow, getRows(headers.asInstanceOf[BasicRequestHeaders])) // TODO: HEADERS HACK
  }
}