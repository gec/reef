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
package org.totalgrid.reef.benchmarks.measurements

import org.totalgrid.reef.client.service.proto.Measurements.Measurement

import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor, SubscriptionResult }
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.sapi.client.Promise
import net.agileautomata.executor4s.{ Success, Result }
import org.totalgrid.reef.util.Timing.Stopwatch
import org.totalgrid.reef.benchmarks.FailedBenchmarkException

/**
 * this utility class watches the measurment stream to time how long it takes for a list of measurements to be seen.
 * It tests that not only does it recieves the measurements but also that they come in the right order. If a measurement
 * is missed it will not notice the rest of the measurements.
 */
class ConcurrentRoundtripTimer(client: Client, result: SubscriptionResult[List[Measurement], Measurement]) extends SubscriptionEventAcceptor[Measurement] {

  result.getSubscription.start(this)

  def cancel() = this.synchronized {
    if (!onMeasurementCallbacks.isEmpty) {
      throw new FailedBenchmarkException("Not all measurement roundtrips completed successfully.")
    }
    result.getSubscription.cancel()
  }

  case class RoundtripReading(firstMessage: Long, lastMessage: Long)

  /**
   * list of promises still awaiting measurements. When a measurement is received it is given
   * to all of the callbacks. Once a callback has gotten all the measurements its looking for
   * it will return false and we filter it out of the list.
   */
  private var onMeasurementCallbacks = List.empty[(Measurement) => Boolean]

  def onEvent(event: SubscriptionEvent[Measurement]) = this.synchronized {
    onMeasurementCallbacks = onMeasurementCallbacks.filter { _(event.getValue) }
  }

  /**
   * returns a promise to a reading that has measured how long much time had elapsed until the first
   * measurement was received and how long it took to receive the last one. All times in milliseconds.
   */
  def timeRoundtrip(measurements: List[Measurement]): Promise[RoundtripReading] = this.synchronized {
    val exe = client.getInternal.getExecutor
    val future = exe.future[Result[RoundtripReading]]

    var expected = measurements
    var firstMeas = Option.empty[Long]
    val stopwatch = new Stopwatch()

    def onMeas(m: Measurement): Boolean = {
      if (expected.head == m) {
        if (firstMeas.isEmpty) firstMeas = Some(stopwatch.elapsed)
        expected = expected.tail
      }
      // when we are not waiting for any more measurements were done and make the lastMessage measurement
      if (expected.isEmpty) {
        future.set(Success(new RoundtripReading(firstMeas.get, stopwatch.elapsed)))
        false
      } else {
        true
      }
    }

    onMeasurementCallbacks ::= onMeas _

    Promise.from(future)
  }

}