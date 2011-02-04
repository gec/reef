package org.totalgrid.reef.protoapi.client

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.protoapi.RequestEnv
import org.totalgrid.reef.protoapi.ProtoServiceTypes._

trait AsyncOperations extends Logging {

  /** All other async functions can be reduced to this
   */
  def asyncRequest[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv)(callback: Option[Response[A]] => Unit)

  /* --- Thick Interface --- All function prevalidate the response code so the client doesn't have to check it */
  def asyncGet[A <: GeneratedMessage](payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncVerbWrapper(Envelope.Verb.GET, payload, env, callback)

  def asyncDelete[A <: GeneratedMessage](payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncVerbWrapper(Envelope.Verb.DELETE, payload, env, callback)

  def asyncPost[A <: GeneratedMessage](payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncVerbWrapper(Envelope.Verb.POST, payload, env, callback)

  def asyncPut[A <: GeneratedMessage](payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncVerbWrapper(Envelope.Verb.PUT, payload, env, callback)

  //def asyncVerb[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncVerbWrapper(verb, payload, env, callback)

  def asyncGetOne[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = asyncGet(payload, env) { checkOne(payload, callback) }

  def asyncDeleteOne[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = asyncDelete(payload, env) { checkOne(payload, callback) }

  def asyncPutOne[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = asyncPut(payload, env) { checkOne(payload, callback) }

  private def asyncVerbWrapper[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv, callback: MultiResult[A] => Unit) {

    def handleResult(resp: Option[Response[A]]) = callback(convert(resp))

    asyncRequest[A](verb, payload, env)(handleResult)

  }

  private def checkOne[T <: GeneratedMessage](request: T, callback: SingleResult[T] => Unit): (MultiResult[T]) => Unit = { (multi: MultiResult[T]) =>
    callback(expectOneResponse[T](request, multi))
  }

  private def expectOneResponse[T <: GeneratedMessage](request: T, response: MultiResult[T]): SingleResult[T] = response match {
    case MultiResponse(List(x)) => SingleResponse(x)
    case MultiResponse(list) =>
      warn { "Unexpected result set size: " + list.size }
      Failure(Envelope.Status.UNEXPECTED_RESPONSE, "Expected one results, but got: " + list.size + " request: " + request)
    case x: Failure => x
  }

}