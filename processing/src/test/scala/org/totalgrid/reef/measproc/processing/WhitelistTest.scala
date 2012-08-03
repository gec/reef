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
package org.totalgrid.reef.measproc.processing

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

import org.totalgrid.reef.measproc.ProtoHelper._
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Point }
import org.totalgrid.reef.jmx.{ MetricsContainer, Metrics }

@RunWith(classOf[JUnitRunner])
class WhitelistTest extends FunSuite with ShouldMatchers {
  test("Ignores meases") {
    val queue = mutable.Queue.empty[String]
    val enqueue = { m: Measurement => queue.enqueue(m.getName) }
    val metrics = Metrics(MetricsContainer())

    def makePoint(name: String) = Point.newBuilder.setName(name).setUuid(ReefUUID.newBuilder.setValue(name)).build
    val filter = new MeasurementWhiteList(enqueue, List(makePoint("ok1"), makePoint("ok2")), metrics)

    filter.process(makeAnalog("ok1", 100))
    filter.process(makeAnalog("ok2", 100))

    filter.process(makeAnalog("bad1", 100))
    filter.process(makeAnalog("bad2", 100))
    filter.process(makeAnalog("bad1", 100))

    filter.process(makeAnalog("ok1", 100))

    queue.toList should equal(List("ok1", "ok2", "ok1"))
  }
}