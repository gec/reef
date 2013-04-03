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
package org.totalgrid.reef.protocol.api.util

import net.agileautomata.executor4s._
import scala.collection.mutable.ArrayBuffer
import org.totalgrid.reef.util.concurrent.AtomicReference

/**
 * provides a connivance class to marshall disparate small events into a joined up list of
 * those events after either a defined time has elapsed or a max number of events have been
 * queued. When events are ready to be published the pubFunc function _will_ be called using
 * the publishingStrand thread. There is no guarente that the publish calls will be exactly
 * maxEntries long, it may be longer if a large batch has been added.
 */
class PackTimer[A](maxTimeMS: Long, maxEntries: Long, pubFunc: List[A] => Unit, publishingStrand: Strand) {

  /*
   * Threading: We use multiple levels of synchronization so that we are never holding a lock
   * the scheduled publisher will need to complete (so cancel can't deadlock). We protect the
   * creation and consumption of the entries by syncying on buffer. We synchronize on the timer
   * as whole to make sure we only scheduling one publish event at a time. Lastly we use an atomic
   * so once we have completed the publishing the publishing thread can notify the producers without
   * locking on the object as a whole.
   */

  private val batch = ArrayBuffer.empty[A]
  private val queuedEvent = new AtomicReference[Cancelable](None)
  private var publishingFullBatch = false
  private var canceled = false

  def addEntry(entry: A) {
    val size = batch.synchronized {
      batch.append(entry)
      batch.size
    }
    updateTimer()
  }
  def addEntries(entries: Seq[A]) {
    val size = batch.synchronized {
      entries.foreach { batch.append(_) }
      batch.size
    }
    updateTimer()
  }

  private def updateTimer() {
    this.synchronized {
      if (!canceled) {
        publishingStrand.execute(
          this.synchronized {
            if (!canceled) {
              possiblySchedulePublish()
            }
          })
      }
    }
  }

  private def possiblySchedulePublish() {

    def reschedulePublish(delay: Long) {
      queuedEvent.getOption.foreach { _.cancel }
      // once cancel has completed we have either published (and cleared queuedEvent) or
      // stopped the execution from happening and events should still be queued
      queuedEvent.set(Some(publishingStrand.schedule(delay.milliseconds) {
        publishQueuedEntries()
        queuedEvent.set(None)
        updateTimer()
      }))
    }

    val size = batch.synchronized { batch.size }
    val isScheduled = !queuedEvent.getOption.isEmpty
    if (isScheduled && publishingFullBatch) {
      // publishing as fast as we can already
    } else if (size >= maxEntries && (!isScheduled || !publishingFullBatch)) {
      reschedulePublish(0)
      publishingFullBatch = true
    } else if (!isScheduled && size > 0) {
      reschedulePublish(maxTimeMS)
      publishingFullBatch = false
    }
  }

  private def publishQueuedEntries() {
    val publishableEntries = batch.synchronized {
      val temp = batch.toList
      batch.clear
      temp
    }
    pubFunc(publishableEntries)
  }

  def cancel() {
    this.synchronized {
      canceled = false
    }
    queuedEvent.getOption.foreach { _.cancel }
  }
}