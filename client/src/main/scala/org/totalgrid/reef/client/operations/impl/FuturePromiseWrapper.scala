package org.totalgrid.reef.client.operations.impl

import net.agileautomata.executor4s.Future
import org.totalgrid.reef.client._

object FuturePromiseWrapper {

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
      val result: Future[Either[Exception, B]] = future.map {
        v =>
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

