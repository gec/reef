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
import org.totalgrid.reef.util.SyncVar

import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor, SubscriptionResult }
import org.totalgrid.reef.benchmarks.FailedBenchmarkException

class MeasurementRoundtripTimer(result: SubscriptionResult[List[Measurement], Measurement]) extends SubscriptionEventAcceptor[Measurement] {
  result.getSubscription.start(this)

  val expected = new SyncVar(List.empty[Measurement])
  var unexpected = List.empty[Measurement]

  var startTime: Long = 0
  var firstMessage = Option.empty[Long]

  def start(meases: List[Measurement]) {
    startTime = System.nanoTime()
    firstMessage = None
    expected.update(meases)
    // make sure the syncvar is not sitting on an empty state
    expected.waitFor(_.size == meases.size)
    unexpected = Nil
  }

  def await() = {
    expected.waitFor(_.size == 0, throwOnFailure = false)
    if (expected.current.size != 0) {
      throw new FailedBenchmarkException("Didn't recieve all expected measurements, still awaiting: " + expected.current + " got unexpected: " + unexpected.reverse)
    }
    (System.nanoTime() - startTime) / 1000000
  }

  def onEvent(event: SubscriptionEvent[Measurement]) {
    if (firstMessage.isEmpty) firstMessage = Some((System.nanoTime() - startTime) / 1000000)
    val meas = event.getValue
    val head = expected.current.head
    if (head.getName == meas.getName && head.getTime == meas.getTime) {
      expected.atomic(_.tail)
    } else {
      unexpected ::= meas
    }
  }
}