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

package org.totalgrid.reef.executor

import parallel.Future
import scala.annotation.tailrec

final class SynchronizedFuture[A] extends Future[A] {

  private var result: Option[Either[Exception, A]] = None

  private val mutex = new Object

  final override def isDone = result.isDefined

  def set(value: Either[Exception, A]) = mutex.synchronized {
    result match {
      case Some(x) =>
        throw new IllegalStateException("Result already defined to be: " + x + ", tried to set it to: " + value)
      case None =>
        result = Some(value)
        mutex.notifyAll()
    }
  }

  final override def apply(): A = {

    @tailrec
    def complete: A = result match {
      case Some(e) => e match {
        case Left(ex) => throw ex
        case Right(value) => value
      }
      case None =>
        mutex.wait()
        complete
    }

    mutex.synchronized {
      complete
    }
  }

}

final class FixedFuture[A](result: A) extends Future[A] {
  def isDone = true
  def apply = result
}
