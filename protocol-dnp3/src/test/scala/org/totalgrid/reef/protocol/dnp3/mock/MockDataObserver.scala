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
package org.totalgrid.reef.protocol.dnp3.mock

import scala.collection.mutable
import org.totalgrid.reef.protocol.dnp3._

class MockDataObserver extends IDataObserver {

  private var inTransaction = false
  override def _Start() = inTransaction = true
  override def _End() = inTransaction = false

  val binaries = new mutable.Queue[(Binary, Long)]
  val analogs = new mutable.Queue[(Analog, Long)]
  val counters = new mutable.Queue[(Counter, Long)]
  val controlStatus = new mutable.Queue[(ControlStatus, Long)]
  val setpointStatus = new mutable.Queue[(SetpointStatus, Long)]

  override def _Update(value: Binary, index: Long) = binaries += ((value, index))
  override def _Update(value: Analog, index: Long) = analogs += ((value, index))
  override def _Update(value: Counter, index: Long) = counters += ((value, index))
  override def _Update(value: ControlStatus, index: Long) = controlStatus += ((value, index))
  override def _Update(value: SetpointStatus, index: Long) = setpointStatus += ((value, index))
}