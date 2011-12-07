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

import org.totalgrid.reef.client.sapi.client.Promise
import net.agileautomata.executor4s.{ Settable, Success, Result }

final class SynchronizedPromise[A] private (private var result: Option[A]) extends Promise[A] with Settable[A] {

  def this(result: A) = this(Some(result))
  def this() = this(None)

  private var listeners: List[Promise[A] => Unit] = Nil
  private val mutex = new Object

  override def isComplete = result.isDefined

  override def set(value: A) = mutex.synchronized {
    result match {
      case Some(x) =>
        throw new IllegalStateException("Result already defined to be: " + result + ", tried to set it to: " + value)
      case None =>
        result = Some(value)
        mutex.notifyAll()
        listeners.foreach(_.apply(this))
        listeners = Nil
    }
  }

  override def map[B](fun: A => B): Promise[B] = new SynchronizedPromise[B](fun(this.await))

  override def await: A = extract.get

  override def extract: Result[A] = mutex.synchronized {
    result match {
      case None =>
        mutex.wait()
        extract
      case Some(x) => Success(x)
    }
  }

  override def listen(fun: Promise[A] => Unit) = mutex.synchronized {
    result match {
      case Some(x) => fun(this)
      case None => listeners = fun :: listeners
    }
    this
  }

}