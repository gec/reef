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
package org.totalgrid.reef.protocol.dnp3.slave

import org.totalgrid.reef.executor.Executor
import scala.collection.mutable.ArrayBuffer
import org.totalgrid.reef.util.Timer

/**
 * provides a connivance class to marshall disparate small events into a joined up list of
 * those events after either a defined time has elapsed or a max number of events have been
 * queued. When events are ready to be published the pubFunc function _will_ be called using
 * the executor thread.
 */
class PackTimer[A](maxTimeMS: Long, maxEntries: Long, pubFunc: List[A] => Unit, executor: Executor) {

  private val batch = ArrayBuffer.empty[A]
  private var queuedEvent: Option[Timer] = None

  def addEntry(entry: A) = this.synchronized {
    batch.append(entry)
    updateTimer
  }

  def addEntries(entries: List[A]) = this.synchronized {
    entries.foreach { batch.append(_) }
    updateTimer
  }

  private def updateTimer {
    if (batch.size >= maxEntries) executor.execute {
      publish
    }
    else if (queuedEvent.isEmpty) {
      queuedEvent = Some(executor.delay(maxTimeMS) {
        publish
      })
    }
  }

  private def publish = this.synchronized {
    pubFunc(batch.toList)
    batch.clear
    queuedEvent.foreach { _.cancel }
    queuedEvent = None
  }

  def cancel() = this.synchronized {
    queuedEvent.foreach { _.cancel }
  }
}