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
package org.totalgrid.reef.calc.lib.eval

object BasicOperations {

  class Sum extends Operation {
    def names = List("SUM", "+")

    def apply(args: List[OperationValue]): OperationValue = {
      val result = args.foldLeft(0.0) {
        case (l, NumericValue(v)) => l + v
        case (l, NumericMeas(v, _)) => l + v
        case (l, x) => throw new EvalException("Sum only takes numeric values")
      }
      NumericValue(result)
    }
  }
}
