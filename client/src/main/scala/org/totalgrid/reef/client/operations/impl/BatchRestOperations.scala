package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.{Promise, RequestHeaders}
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.operations.RestOperations
import org.totalgrid.reef.client.sapi.client.rest.impl.ClassLookup
import java.util.UUID
import com.google.protobuf.ByteString
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.types.{TypeDescriptor, ServiceTypeInformation}
import collection.mutable.Queue
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.operations.scl.ScalaPromise._
import org.totalgrid.reef.client.operations.scl.ScalaResponse._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.proto.{StatusCodes, Envelope}
import org.totalgrid.reef.client.javaimpl.ResponseWrapper
import org.totalgrid.reef.client.proto.Envelope.{ServiceResponse, BatchServiceRequest, SelfIdentityingServiceRequest, Verb}
import org.totalgrid.reef.client.exception.{InternalClientError, ReefServiceException}

trait BatchRestOperations extends RestOperations {
  def flush(): Promise[BatchServiceRequest]
  def batchedFlush(batchSize: Int): Promise[Boolean]
}

trait DefaultBatchRestOperations extends BatchRestOperations with DerivedRestOperations {
  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _]
  protected def exe: Executor
  protected def ops: RestOperations

  case class QueuedRequest[A](request: SelfIdentityingServiceRequest, descriptor: TypeDescriptor[A], promise: OpenPromise[Response[A]])
  private val requestQueue = Queue.empty[QueuedRequest[_]]

  protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {

    val descriptor: TypeDescriptor[A] = getServiceInfo(ClassLookup.get(payload)).getDescriptor
    val uuid = UUID.randomUUID().toString

    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid)
    builder.setPayload(ByteString.copyFrom(descriptor.serialize(payload)))
    headers.foreach { _.asInstanceOf[BasicRequestHeaders].toEnvelopeRequestHeaders.foreach(builder.addHeaders) } // TODO: HACK HACK HACK

    val request = SelfIdentityingServiceRequest.newBuilder.setExchange(descriptor.id).setRequest(builder).build

    //val promise: OpenPromise[Response[A]] = FuturePromise.open(exe.future[Either[ReefServiceException, Response[A]]])
    val promise: OpenPromise[Response[A]] = FuturePromise.open[Response[A]](exe)

    requestQueue.enqueue(QueuedRequest[A](request, descriptor, promise))

    //hook.notifyRequestSpys(verb, payload, future)  // TODO: put this back in
    promise
  }


  def flush(): Promise[BatchServiceRequest] = {
    sendBatch(popRequests(), None)
  }


  def batchedFlush(batchSize: Int): Promise[Boolean] = {

    def nextBatch(prevFailed: Option[ReefServiceException], pending: List[QueuedRequest[_]], promise: OpenPromise[Boolean]) {
      prevFailed match {
        case Some(rse) => promise.setFailure(rse)
        case None => pending match {
          case Nil => promise.setSuccess(true)
          case remains =>
            val (now, later) = remains.splitAt(batchSize)
            sendBatch(now, Some(nextBatch(_, later, promise)))
        }
      }
    }

    val promise = FuturePromise.open[Boolean](exe)

    nextBatch(None, popRequests(), promise)

    promise
  }

  private def sendBatch(requests: List[QueuedRequest[_]], chain: Option[(Option[ReefServiceException]) => Unit]): Promise[BatchServiceRequest] = {

    def applyResponseToPromise[A](response: ServiceResponse, desc: TypeDescriptor[A], promise: OpenPromise[Response[A]]) {
      StatusCodes.isSuccess(response.getStatus) match {
        case true =>
          val data = response.getPayloadList.toList.map(bs => desc.deserialize(bs.toByteArray))
          promise.setSuccess(ResponseWrapper.success(response.getStatus, data))
        case false =>
          promise.setFailure(new ReefServiceException(response.getErrorMessage, response.getStatus))
      }
    }

    val batch = {
      val b = BatchServiceRequest.newBuilder
      requests.foreach(r => b.addRequests(r.request))
      b.build
    }

    val batchPromise: Promise[Response[BatchServiceRequest]] = ops.request(Envelope.Verb.POST, batch)

    batchPromise.listenEither {
      case Right(resp) => {
        val responses = resp.getList.get(0).getRequestsList.toList.map(_.getResponse)
        responses.zip(requests).foreach {
          case (servResp, QueuedRequest(_, desc, promise)) => applyResponseToPromise(servResp, desc, promise)
        }
        chain.foreach(_(None))
      }
      case Left(ex) => {
        val rse = ex match {
          case rse: ReefServiceException => rse
          case other => new InternalClientError("Problem with batch request", ex)
        }
        requests.foreach(_.promise.setFailure(rse))
        requests.foreach(_.promise.await) // make sure all of the sub future listen() calls have fired
        chain.foreach(_(Some(rse)))
      }
    }

    batchPromise.map(_.one)
  }


  private def popRequests(): List[QueuedRequest[_]] = {
    val list = requestQueue.toList
    requestQueue.clear()
    list
  }
}


