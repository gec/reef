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
package org.totalgrid.reef.util

import scala.collection.mutable.Queue

class BlockingQueue[A] {

  private val queue = new Queue[A]

  def push(o: A): Unit = {
    queue.synchronized {
      queue.enqueue(o)
      queue.notify()
    }
  }

  def pop(timeout: Long): A = {
    queue.synchronized {
      if (queue.size == 0) queue.wait(timeout)
      queue.dequeue()

    }
  }

  /**
   * Block for timeout milliseconds until the queue has _atleast_ size entries (may have more than size)
   * @return whether the queue has atleast size entries
   */
  def waitUntil(size: Int, timeout: Long): Boolean = {
    queue.synchronized {
      val end = System.currentTimeMillis + timeout
      var waitFor = timeout
      while (waitFor > 0 && queue.size < size) {
        queue.wait(waitFor)
        waitFor = end - System.currentTimeMillis
      }
      queue.size >= size
    }
  }

  def size: Int = queue.size

}

