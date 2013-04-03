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
package org.totalgrid.reef.models

import org.squeryl.dsl.ast.{ BinaryOperatorNodeLogicalBoolean, LogicalBoolean }
import org.totalgrid.reef.client.exception.BadRequestException

object SquerylConversions {

  class NoSearchTermsException(msg: String) extends BadRequestException(msg)

  /**
   *  Common logic for dynamically combining multiple squeryl expressions (with and)
   * @param exps    List of squeryl expressions
   * @return        Expression that results from intersection of input expressions
   */
  def combineExpressions(exps: List[LogicalBoolean]) = {
    exps.length match {
      case 0 => throw new NoSearchTermsException("No search terms in query. If searching for all records use a wildcard (\"*\") on a searchable field")
      case _ =>
        exps.reduceLeft { (a, b) =>
          new BinaryOperatorNodeLogicalBoolean(a, b, "and")
        }
    }
  }

  implicit def expressionAnder(exps: List[LogicalBoolean]): LogicalBoolean = combineExpressions(exps)
}