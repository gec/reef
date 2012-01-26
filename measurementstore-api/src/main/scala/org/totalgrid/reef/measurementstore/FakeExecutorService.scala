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
package org.totalgrid.reef.measurementstore

import net.agileautomata.executor4s._

/**
 * promotes a simple executor to an ExecutorService by implementing
 * terminate and shutdown.
 *
 * TODO: move FakeExecutorService into executor4s
 */
class FakeExecutorService(delegate: Executor) extends ExecutorService {

  private var executor: Option[Executor] = Some(delegate)
  private def exe = executor.getOrElse(throw new Exception("Rejected execution"))

  def operationTimeout = exe.operationTimeout

  def execute(fun: => Unit) = exe.execute(fun)

  def attempt[A](fun: => A) = exe.attempt(fun)

  def schedule(interval: TimeInterval)(fun: => Unit) = exe.schedule(interval)(fun)

  def scheduleWithFixedOffset(initial: TimeInterval, offset: TimeInterval)(fun: => Unit) =
    exe.scheduleWithFixedOffset(initial, offset)(fun)

  def shutdown() = executor = None

  protected def terminate(interval: TimeInterval) = {
    shutdown()
    true
  }
}