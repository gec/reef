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
package org.totalgrid.reef.client.operations.impl

import net.agileautomata.executor4s.Future
import org.totalgrid.reef.client._

object FuturePromise {

  def apply[A](fut: Future[A]) = new InitialPromise(fut)

  trait DefinedPromise[A] extends Promise[A] {
    protected def original: Promise[A]

    def listen(listener: PromiseListener[A]) {
      original.listen(listener)
    }

    def isComplete: Boolean = true

    def transform[B](trans: PromiseTransform[A, B]): Promise[B] = {
      original.transform(trans)
    }
  }

  class DefinedInitialPromise[A](value: A, protected val original: Promise[A]) extends DefinedPromise[A] {
    def await(): A = value
  }

  class DefinedEitherPromise[A](value: Either[Exception, A], protected val original: Promise[A]) extends DefinedPromise[A] {
    def await(): A = value match {
      case Left(ex) => throw ex
      case Right(v) => v
    }
  }

  class InitialPromise[A](future: Future[A]) extends Promise[A] {

    def await(): A = future.await

    def listen(listener: PromiseListener[A]) {
      // we can't pass our listeners this promise because if try to extract
      // or await on the value it will deadlock the future which is waiting
      // for the all of the listen callbacks to complete.
      future.listen(result => listener.onComplete(new DefinedInitialPromise[A](result, this)))
    }

    def isComplete: Boolean = future.isComplete

    def transform[B](trans: PromiseTransform[A, B]): Promise[B] = {
      val result: Future[Either[Exception, B]] = future.map { v =>
        try {
          Right(trans.transform(v))
        } catch {
          case ex: Exception => Left(ex)
        }
      }
      new EitherPromise(result)
    }
  }

  class EitherPromise[A](future: Future[Either[Exception, A]]) extends Promise[A] {
    def await(): A = future.await match {
      case Left(ex) => throw ex
      case Right(v) => v
    }

    def listen(listener: PromiseListener[A]) {
      // we can't pass our listeners this promise because if try to extract
      // or await on the value it will deadlock the future which is waiting
      // for the all of the listen callbacks to complete.
      future.listen(result => listener.onComplete(new DefinedEitherPromise[A](result, this)))
    }

    def isComplete: Boolean = future.isComplete

    def transform[B](trans: PromiseTransform[A, B]): Promise[B] = {
      val result = future.map {
        case Right(v) => Right(trans.transform(v))
        case left => left.asInstanceOf[Either[Exception, Nothing]]
      }
      new EitherPromise[B](result)
    }
  }
}

