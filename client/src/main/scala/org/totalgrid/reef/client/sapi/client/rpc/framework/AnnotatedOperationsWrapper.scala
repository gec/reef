package org.totalgrid.reef.client.sapi.client.rpc.framework

import org.totalgrid.reef.client.sapi.service.AsyncService
import org.totalgrid.reef.client.sapi.client.rest.{RestOperations => SRestOperations, AnnotatedOperations}
import net.agileautomata.executor4s.{Result, Future}
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.sapi.client.{Subscription, Promise => SPromise}
import org.totalgrid.reef.client.{Promise, SubscriptionResult, SubscriptionBinding}
import org.totalgrid.reef.client.operations._

class AnnotatedOperationsWrapper(japi: ServiceOperations) extends AnnotatedOperations {

  def operation[A](err: => String)(fun: (SRestOperations) => Future[Result[A]]): SPromise[A] = {
    /*val msg = new ErrorMessage {
      def build(): String = err
    }
    val op = new BasicOperation {
      def execute[T](operations: RestOperations): Promise[Response[T]] = {
        fun()
      }
    }

    japi.operation() */

    null
  }

  def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (Subscription[B], SRestOperations) => Future[Result[A]]): Promise[SubscriptionResult[A, B]] = {
    null
  }

  def clientSideService[A, B](handler: AsyncService[B], err: => String)(fun: (SubscriptionBinding, SRestOperations) => Future[Result[A]]): Promise[SubscriptionBinding] = {
    null
  }
}
