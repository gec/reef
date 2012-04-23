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
package org.totalgrid.reef.broker.memory

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.BrokerConnectionFactory

final class MemoryBrokerConnectionFactory(exe: Executor) extends BrokerConnectionFactory {

  import MemoryBrokerState._
  private var state = State()
  private var updating = false

  // This call should never be made re-entrantly
  def update(fun: MemoryBrokerState.State => MemoryBrokerState.State) = synchronized {
    if (updating) {
      throw new RuntimeException("Memory broker used re-entrantly")
    }
    updating = true
    state = fun(state)
    updating = false
  }

  //def getState: MemoryBrokerState.State = state

  def connect = new MemoryBrokerConnection(this, exe)

  override def toString() = "in-memory-broker"
}
