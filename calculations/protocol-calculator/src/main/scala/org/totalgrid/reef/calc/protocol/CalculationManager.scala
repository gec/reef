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
package org.totalgrid.reef.calc.protocol

import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.service.proto.Model.ReefUUID
import org.totalgrid.reef.app.{ SubscriptionHandlerBase, ServiceContext }
import com.typesafe.scalalogging.slf4j.Logging
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.calc.lib.CalculationFactory

// implicitly synchronized since the subscription events come in on only a single strand
class CalculationManager(factory: CalculationFactory)
    extends SubscriptionHandlerBase[Calculation] with ServiceContext[Calculation] with Logging {

  private var calcs = Map.empty[ReefUUID, Cancelable]

  def add(obj: Calculation) = {
    logger.info("Adding calculation for point: " + obj.getOutputPoint.getName)
    calcs += obj.getUuid -> factory.build(obj)
  }

  def remove(obj: Calculation) = remove(obj.getUuid)

  def modify(obj: Calculation) = {
    remove(obj)
    add(obj)
  }

  def subscribed(list: List[Calculation]) = list.foreach(add(_))

  def clear() = calcs.keys.foreach(remove(_))

  private def remove(uuid: ReefUUID) = {
    calcs.get(uuid).foreach { calculator =>
      calculator.cancel()
      calcs -= uuid
    }
  }
}
