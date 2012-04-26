package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.{Promise, RequestHeaders}
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.operations.RestOperations

/*
trait BatchRestOperations extends RestOperations {

}

class DefaultBatchRestOperations extends BatchRestOperations {

  protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {
    null
  }
}
 */
/*

class BatchServiceRestOperations(ops: RestOperations, hook: RequestSpyHook, registry: ServiceRegistry, exe: Executor) extends RestOperations {

  case class RequestWithFuture[A](request: SelfIdentityingServiceRequest, future: Future[Response[A]] with Settable[Response[A]], descriptor: TypeDescriptor[A])
  private val pendingRequests = Queue.empty[RequestWithFuture[_]]

  def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {

    val info = registry.getServiceInfo(ClassLookup.get(payload))
    val uuid = UUID.randomUUID().toString

    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid)
    builder.setPayload(ByteString.copyFrom(info.getDescriptor.serialize(payload)))
    headers.foreach { _.toEnvelopeRequestHeaders.foreach(builder.addHeaders) }

    val cachedRequest = SelfIdentityingServiceRequest.newBuilder.setExchange(info.getDescriptor.id).setRequest(builder).build

    val future = exe.future[Response[A]]

    pendingRequests.enqueue(RequestWithFuture(cachedRequest, future, info.getDescriptor))
    hook.notifyRequestSpys(verb, payload, future)

    future
  }

  /**
   * send all of the pending requests in a single BatchServiceRequests
   */
  def flush() = {
    doBatchRequest(grabPendingRequests(), None)
  }

  /**
   * sends all of the pending requests in multiple BatchServiceRequests of no more than batchSize operations per request.
   * Future blocks until all of the operations have completed (which may take a long time)
   */
  def batchedFlush(batchSize: Int) = {

    def startNextBatch(future: SettableFuture[Response[Boolean]], pending: List[RequestWithFuture[_]], failure: Option[FailureResponse]) {

      if (pending.isEmpty || failure.isDefined) {
        future.set(failure.getOrElse(SuccessResponse(list = List(true))))
      } else {
        val (inProgress, remaining) = pending.splitAt(batchSize)

        doBatchRequest(inProgress, Some(startNextBatch(future, remaining, _)))
      }
    }

    val overallFuture = exe.future[Response[Boolean]]

    startNextBatch(overallFuture, grabPendingRequests(), None)

    Promise.from(overallFuture.map { _.one })
  }

  private def grabPendingRequests() = {
    val pending = pendingRequests.toList
    pendingRequests.clear()
    pending
  }

  private def doBatchRequest(inProgress: List[RequestWithFuture[_]], onResult: Option[(Option[FailureResponse]) => Unit]) = {

    val b = BatchServiceRequest.newBuilder
    inProgress.foreach { o => b.addRequests(o.request) }
    val batchServiceProto = b.build

    val batchFuture = ops.request(Envelope.Verb.POST, batchServiceProto, None)
    batchFuture.listen {
      _ match {
        case SuccessResponse(status, batchResults) =>
          val resultAndFuture = batchResults.head.getRequestsList.toList.map { _.getResponse }.zip { inProgress }
          resultAndFuture.foreach {
            case (response, reqWithFuture) => applyResponse(response, reqWithFuture)
          }
          onResult.foreach { _(None) }
          SuccessResponse(status, batchResults)
        case fail: FailureResponse =>
          inProgress.foreach { _.future.set(fail) }
          // make sure all of the sub future listen() calls have fired
          inProgress.foreach { _.future.await }
          onResult.foreach { _(Some(fail)) }
          fail
      }
    }
    Promise.from(batchFuture.map { _.one })
  }

  private def applyResponse[A](response: ServiceResponse, reqWithFuture: RequestWithFuture[A]) {
    val successOrFailure = if (StatusCodes.isSuccess(response.getStatus)) {
      val data: List[A] = response.getPayloadList.toList.map { bs => reqWithFuture.descriptor.deserialize(bs.toByteArray) }
      SuccessResponse(response.getStatus, data)
    } else {
      FailureResponse(response.getStatus, response.getErrorMessage)
    }
    reqWithFuture.future.set(successOrFailure)
  }
}
 */
