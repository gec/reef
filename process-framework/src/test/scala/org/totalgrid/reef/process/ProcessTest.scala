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
package org.totalgrid.reef.process

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import net.agileautomata.executor4s._
import net.agileautomata.commons.testing.SynchronizedVariable

@RunWith(classOf[JUnitRunner])
class ProcessTestSuite extends FunSuite with ShouldMatchers {

  def fixture[A](test: Executor => A): A = {
    val exe = Executors.newScheduledThreadPool()
    exe.addExceptionHandler(LoggingExceptionHandler)
    try { test(exe) }
    finally { exe.terminate() }
  }


  test("Process that throws exception on start is automatically retryed") {
    fixture { exe =>
      val f = new ProcessCoordinator
      val s = Strand(exe)
      val count = new SynchronizedVariable[Int](0)

      f.retry(s, 10.milliseconds) { events =>
        new ProcessBinding {
          count.modify(_ + 1)
          throw new Exception("Unable to start")
          def shutdown() = events.onStop()
        }
      }

      count shouldBecome(2) within 500
    }

  }

}
