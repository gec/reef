/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.executor.mock

import org.totalgrid.reef.executor._
import org.totalgrid.reef.util.Timer

/**
 * Mock reactor for use in testing that runs all commands instantly, no delays, no repetition,
 * all on the calling thread. A check is done to ensure that an infinite loop is not entered. This
 * class is not appropriate for testing class with with complex timing requirements
 */

class InstantExecutor extends Executor with Lifecycle {

  private var count = 0

  private def checkDepth[A](fun: => A): A = {
    count += 1
    try {
      if (count < 100) fun
      else throw new Exception("Infinite recursion detected")
    } finally {
      count -= 1
    }
  }

  object NullTimer extends Timer {
    def cancel = {}
    def now = {}
  }

  override def execute(fun: => Unit): Unit = checkDepth(fun)

  override def delay(msec: Long)(fun: => Unit): Timer = { checkDepth(fun); NullTimer }

  override def repeat(msec: Long)(fun: => Unit): Timer = { checkDepth(fun); NullTimer }

  override def request[A](fun: => A): A = checkDepth(fun)

}
