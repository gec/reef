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

import org.totalgrid.reef.client._
import exception.{ InternalClientError, ReefServiceException }
import net.agileautomata.executor4s.{ SettableFuture, Executor, Future }

trait OpenPromise[A] extends Promise[A] {
  def setSuccess(v: A)
  def setFailure(ex: ReefServiceException)
}

object FuturePromise {

  def apply[A](fut: Future[A]): Promise[A] = new ClosedInitialPromise(fut)

  def error[A](err: ReefServiceException, exe: Executor): Promise[A] = {
    val future = exe.future[Either[ReefServiceException, A]]
    future.set(Left(err))
    new ClosedEitherPromise(future)
  }

  def open[A](settable: SettableFuture[Either[ReefServiceException, A]]): OpenPromise[A] = new OpenEitherPromise[A](settable)
  def open[A](exe: Executor): OpenPromise[A] = new OpenEitherPromise[A](exe.future[Either[ReefServiceException, A]])

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
    def transformError(transform: PromiseErrorTransform): Promise[A] = this
  }

  class DefinedEitherPromise[A](value: Either[ReefServiceException, A], protected val original: Promise[A]) extends DefinedPromise[A] {
    def await(): A = value match {
      case Left(ex) => throw ex
      case Right(v) => v
    }
    def transformError(transform: PromiseErrorTransform): Promise[A] = value match {
      case Right(_) => this
      case Left(ex) => new DefinedEitherPromise(Left(transform.transformError(ex)), this)
    }
  }

  def performTransform[A, B](v: A, trans: PromiseTransform[A, B]): Either[ReefServiceException, B] = {
    try {
      Right(trans.transform(v))
    } catch {
      case rse: ReefServiceException => Left(rse)
      case ex: Exception => Left(new InternalClientError("Unexpected error: " + ex.getMessage, ex))
    }
  }

  trait InitialPromise[A] extends Promise[A] {
    protected def future: Future[A]

    def await(): A = future.await

    def listen(listener: PromiseListener[A]) {
      // we can't pass our listeners this promise because if try to extract
      // or await on the value it will deadlock the future which is waiting
      // for the all of the listen callbacks to complete.
      future.listen(result => listener.onComplete(new DefinedInitialPromise[A](result, this)))
    }

    def isComplete: Boolean = future.isComplete

    def transform[B](trans: PromiseTransform[A, B]): Promise[B] = {
      val result = future.map(performTransform(_, trans))
      new ClosedEitherPromise(result)
    }

    def transformError(transform: PromiseErrorTransform): Promise[A] = this
  }

  class ClosedInitialPromise[A](protected val future: Future[A])
    extends InitialPromise[A]

  // Not being used at the moment
  /*class OpenInitialPromise[A](protected val future: SettableFuture[A]) extends InitialPromise[A] with OpenPromise[A] {
    def set(v: A) {
      future.set(v)
    }
    def setFailure(ex: ReefServiceException)
  }*/

  class OpenEitherPromise[A](protected val future: SettableFuture[Either[ReefServiceException, A]]) extends EitherPromise[A] with OpenPromise[A] {
    def setSuccess(v: A) { future.set(Right(v)) }

    def setFailure(ex: ReefServiceException) { future.set(Left(ex)) }
  }

  trait EitherPromise[A] extends Promise[A] {
    protected def future: Future[Either[ReefServiceException, A]]

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
        case Right(v) => performTransform(v, trans)
        case left => left.asInstanceOf[Either[ReefServiceException, Nothing]]
      }
      new ClosedEitherPromise[B](result)
    }

    def transformError(transform: PromiseErrorTransform): Promise[A] = {
      val result = future.map {
        case Left(ex) => Left(transform.transformError(ex))
        case right => right
      }
      new ClosedEitherPromise[A](result)
    }
  }

  class ClosedEitherPromise[A](protected val future: Future[Either[ReefServiceException, A]]) extends EitherPromise[A]
}

