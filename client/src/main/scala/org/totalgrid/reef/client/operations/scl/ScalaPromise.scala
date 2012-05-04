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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.{ PromiseListener, PromiseErrorTransform, PromiseTransform, Promise }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.operations.impl.FuturePromise
import com.weiglewilczek.slf4s.Logging

trait ScalaPromise {

  class RichPromise[A](p: Promise[A]) {
    // TODO: TAKE OUT
    assert(p != null)

    def map[B](f: A => B): Promise[B] = {
      p.transform(new PromiseTransform[A, B] {
        def transform(value: A): B = f(value)
      })
    }

    def mapError(f: ReefServiceException => ReefServiceException): Promise[A] = {
      p.transformError(new PromiseErrorTransform {
        def transformError(error: ReefServiceException): ReefServiceException = f(error)
      })
    }

    def listenFor(f: Promise[A] => Unit) {
      p.listen(new PromiseListener[A] {
        def onComplete(promise: Promise[A]) {
          f(promise)
        }
      })
    }

    // TODO: the ugliness of this and the need for OpenPromise maybe call for a scala
    // TODO: promise type that gets used in the batch impl/other systems
    def listenEither(f: Either[Throwable, A] => Unit) {
      p.listen(new PromiseListener[A] {
        def onComplete(promise: Promise[A]) {
          val unexcept = try {
            Right(promise.await)
          } catch {
            case ex => Left(ex)
          }
          f(unexcept)
        }
      })

    }

  }

  implicit def _scalaPromise[A](p: Promise[A]): RichPromise[A] = new RichPromise(p)
}

object ScalaPromise extends ScalaPromise with Logging {

  // Gathers, using the first error as its failure case
  def collate[A](exe: Executor, promises: List[Promise[A]]): Promise[List[A]] = {

    val promise = FuturePromise.open[List[A]](exe)
    val size = promises.size
    val map = collection.mutable.Map.empty[Int, Promise[A]]

    def gather(i: Int)(prom: Promise[A]) {
      map.synchronized {
        map.put(i, prom)
        if (map.size == size) {
          val all = promises.indices.map(map(_))
          try {
            promise.setSuccess(all.map(_.await()).toList)
          } catch {
            case ex: ReefServiceException => promise.setFailure(ex)
            case x: Exception =>
              logger.error("Unhandled exception: " + x, x)
          }
        }
      }
    }

    if (promises.isEmpty) promise.setSuccess(Nil)
    else promises.zipWithIndex.foreach { case (prom, i) => prom.listenFor(gather(i)) }
    promise
  }
}
