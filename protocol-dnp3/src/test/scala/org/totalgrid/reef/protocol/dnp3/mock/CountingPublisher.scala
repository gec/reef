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
import scala.annotation.tailrec
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.protocol.dnp3._

class CountingPublisher {

  val map = mutable.Map.empty[MockPublisher, Int]

  class MockPublisher extends IDataObserver with Logging {

    private var num = 0

    private def incr(): Int = {
      num += 1
      num
    }

    override def _Update(v: Analog, index: Long) = num += 1
    override def _Update(v: Binary, index: Long) = num += 1
    override def _Update(v: ControlStatus, index: Long) = num += 1
    override def _Update(v: Counter, index: Long) = num += 1
    override def _Update(v: SetpointStatus, index: Long) = num += 1

    override def _End() = map.synchronized {
      logger.debug("Processing batch of size: " + num)
      map.get(this) match {
        case Some(x) => map += this -> (x + num)
        case None => map += this -> num
      }
      map.notifyAll
    }

    override def _Start() { num = 0 }
  }

  def newPublisher: IDataObserver = map.synchronized {
    val pub = new MockPublisher
    map += pub -> 0
    pub
  }

  def waitForMinMessages(min: Int, wait: Long): Boolean = map.synchronized {
    val end = System.currentTimeMillis + wait
    @tailrec
    def waitForMinMessages: Boolean = {
      if (getMin >= min) true
      else {
        val remaining = end - System.currentTimeMillis
        if (remaining > 0) {
          map.wait(remaining)
          waitForMinMessages
        } else false
      }
    }
    waitForMinMessages
  }

  private def getMin: Int = map.values.foldLeft(Int.MaxValue)((x, y) => if (y < x) y else x)

}

