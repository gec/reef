package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.operations.{BasicOperation, RestOperations, ServiceOperations}


object ScalaServiceOperations {

  class RichServiceOperations(ops: ServiceOperations) {

    def operation[A](err: => String)(f: RestOperations => Promise[A]): Promise[A] = {
      ops.operation(new BasicOperation[A] {
        def errorMessage(): String = err

        def execute(operations: RestOperations): Promise[A] = f(operations)
      })
    }
  }

  implicit def _scalaServiceOperations[A](ops: ServiceOperations): RichServiceOperations = {
    new RichServiceOperations(ops)
  }

}

/*
/**
   * Perform an operation with a RestOperations class.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client to generate a result
   */
  def operation[A](err: => String)(fun: RestOperations => Future[Result[A]]): Promise[A]

  /**
   * Similiar to operation. Does a rest operation with a subscription. If the operation fails, the subscription is automatically canceled
   * @param desc TypeDescriptor for the subscription type.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client and subscription to generate a result
   */
  def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (Subscription[B], RestOperations) => Future[Result[A]]): Promise[SubscriptionResult[A, B]]

  /**
   * Does a rest operation with a localServiceBinding. Almost the same as a subscription, but we keep track of the sender
   * and respond back to the requester.
   * @param handler Service handler for all client handled service requests.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client and subscription to generate a result
   */
  def clientSideService[A, B](handler: AsyncService[B], err: => String)(fun: (SubscriptionBinding, RestOperations) => Future[Result[A]]): Promise[SubscriptionBinding]
 */
