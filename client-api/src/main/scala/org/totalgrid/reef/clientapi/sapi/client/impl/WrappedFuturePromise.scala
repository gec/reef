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
package org.totalgrid.reef.clientapi.sapi.client.impl

import org.totalgrid.reef.clientapi.sapi.client.Promise
import net.agileautomata.executor4s.{ Failure, Success, Future, Result }

class WrappedFuturePromise[A](future: Future[Result[A]]) extends Promise[A] {

  def await: A = future.await.get
  def listen(fun: Result[A] => Unit) = {
    future.listen(result => fun(result))
    this
  }
  def extract: Result[A] = future.await
  def map[B](fun: A => B) = Promise.from(future.map(r => r.map(fun)))
  def isComplete: Boolean = future.isComplete

  def flatMap[B](fun: A => Promise[B]): Promise[B] = {
    val f = future.replicate[Result[B]]
    val promise = Promise.from(f)
    def onResult(a: Result[A]) = a match {
      case Success(x) => fun(x).listen(f.set)
      case fail: Failure => f.set(fail)
    }
    future.listen(onResult)
    promise
  }
}
