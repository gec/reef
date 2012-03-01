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
package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Calculations.OutputTime

trait TimeStrategy {
  def getTime(inputs: Map[String, List[Measurement]]): Long
}

/*
message OutputTime{
    enum Strategy{
        MOST_RECENT     = 1;
        AVERAGE_TIME    = 2;
    }
    optional Strategy strategy        = 1;
}
 */

object TimeStrategy {
  def build(config: OutputTime.Strategy) = config match {
    case _ => new MostRecent
    //case _ => throw new Exception("Unknown time strategy")
  }

  class MostRecent extends TimeStrategy {
    def getTime(inputs: Map[String, List[Measurement]]): Long = {
      val time = inputs.values.flatten.map(_.getTime).foldLeft(0L) {
        case (l, r) => if (l >= r) l else r
      }
      if (time != 0) {
        time
      } else {
        System.currentTimeMillis()
      }
    }
  }
}
