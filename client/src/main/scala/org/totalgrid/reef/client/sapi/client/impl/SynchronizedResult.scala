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
package org.totalgrid.reef.client.sapi.client.impl

import net.agileautomata.executor4s.{ Success, Result }

final class SynchronizedResult[A] private (private var result: Option[A]) {

  def this(result: A) = this(Some(result))
  def this() = this(None)

  private val mutex = new Object

  def isComplete = result.isDefined

  def set(value: A) {
    mutex.synchronized {
      result match {
        case Some(x) =>
          throw new IllegalStateException("Result already defined to be: " + result + ", tried to set it to: " + value)
        case None =>
          result = Some(value)
          mutex.notifyAll()
      }
    }
  }

  def await: A = extract.get

  def extract: Result[A] = mutex.synchronized {
    result match {
      case None =>
        mutex.wait()
        extract
      case Some(x) => Success(x)
    }
  }

}