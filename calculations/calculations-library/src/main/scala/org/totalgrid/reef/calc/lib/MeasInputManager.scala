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

import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.sapi.rpc.MeasurementService
import org.totalgrid.reef.client.{ Subscription, SubscriptionResult, SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Calculations.CalculationInput

object MeasInputManager {
  /**
   * go through the list of buckets and return a map of tall the snapshot values iff they are all valid
   */
  def getSnapshot(buckets: List[InputBucket]): Option[Map[String, List[Measurement]]] = {
    // we use foldLeft instead of map for efficiency so we can short circuit eval after first None
    buckets.foldLeft[Option[Map[String, List[Measurement]]]](Some(Map.empty[String, List[Measurement]])) { (m, b) =>
      m match {
        case Some(map) =>
          b.getSnapshot match {
            case Some(data) => Some(map + (b.variable -> data))
            case None => None
          }
        case None => None
      }
    }
  }
}

class MeasInputManager extends InputManager {

  private var buckets: List[InputBucket] = Nil
  private var subscriptions: List[Subscription[Measurement]] = Nil
  private var trigger: Option[EventedTriggerStrategy] = None

  def init(client: Client, config: List[CalculationInput], trigger: Option[EventedTriggerStrategy]) {
    import InputBucket._

    val srv = client.getRpcInterface(classOf[MeasurementService])

    def handleSubResult(subResult: SubscriptionResult[List[Measurement], Measurement], buck: InputBucket): Subscription[Measurement] = {
      subResult.getResult.foreach { m =>
        buck.onReceived(m)
        trigger.foreach(_.handle(m))
      }
      val sub = subResult.getSubscription
      sub.start(new SubscriptionEventAcceptor[Measurement] {
        def onEvent(event: SubscriptionEvent[Measurement]) {
          buck.onReceived(event.getValue)
          trigger.foreach(_.handle(event.getValue))
        }
      })
      sub
    }

    val cfgs = config.map(InputBucket.build(_))

    val buckets = cfgs.map(_.bucket)

    this.buckets = buckets

    val subscriptions = cfgs.map {
      case InputConfig(point, variable, req, buck) =>
        req match {
          case SingleLatest => {
            handleSubResult(srv.subscribeToMeasurementsByNames(List(point)).await, buck)
          }
          case MultiSince(from, limit) => {
            // TODO: meaningful limit, standardize calculating relative time => absolute
            val time = System.currentTimeMillis() + from
            handleSubResult(srv.subscribeToMeasurementHistoryByName(point, time, limit).await, buck)
          }
          case MultiLimit(count) => {
            handleSubResult(srv.subscribeToMeasurementHistoryByName(point, count).await, buck)
          }
        }
    }

    this.subscriptions = subscriptions
    this.trigger = trigger
  }

  def getSnapshot: Option[Map[String, List[Measurement]]] = {
    MeasInputManager.getSnapshot(buckets)
  }

  def cancel() {
    subscriptions.foreach(_.cancel())
  }
}
