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
package org.totalgrid.reef.test

import java.util.concurrent.{ LinkedBlockingDeque, TimeUnit, BlockingQueue => JBlockingQueue }
import java.lang.IllegalStateException

object BlockingQueue {
  def empty[A] = new BlockingQueue[A](new LinkedBlockingDeque[A])
}

class BlockingQueue[A](queue: JBlockingQueue[A]) {

  def push(value: A): Unit = queue.put(value)

  def pop(timeout: Long): A = Option(queue.poll(timeout, TimeUnit.MILLISECONDS)) match {
    case Some(x) => x
    case None => throw new IllegalStateException("No value in queue within " + timeout + " milliseconds")
  }

  def size: Int = queue.size

}
