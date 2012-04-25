package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.{PromiseErrorTransform, PromiseTransform, Promise}


object ScalaPromise {

  class RichPromise[A](p: Promise[A]) {
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
  }

  implicit def _scalaPromise[A](p: Promise[A]): RichPromise[A] = new RichPromise(p)
}
