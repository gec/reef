/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protoapi.client

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, ServiceHandlerHeaders, RequestEnv }

import ProtoServiceTypes._

import org.totalgrid.reef.util.{ Logging, Timing }
import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage

object ServiceClient {
  // These evaluation functions can be made more granular is desired
  def isSuccess(status: Envelope.Status): Boolean = {
    status match {
      case Envelope.Status.OK => true
      case Envelope.Status.CREATED => true
      case Envelope.Status.UPDATED => true
      case Envelope.Status.DELETED => true
      case Envelope.Status.NOT_MODIFIED => true
      case _ => false
    }
  }
}

/** Provides a thick interface full of helper functions via implement the single abstract request function
 */
trait ServiceClient extends SyncServiceClient with Logging {

  import ServiceClient._

  type ResponseFuture[T <: GeneratedMessage] = () => Option[Response[T]]

  /** All other request functions boil down to using this on in some way
   */
  def request[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: RequestEnv, callback: TypedResponseCallback[T])

  /** The default request headers */
  var defaultEnv: Option[RequestEnv] = None

  /** Set the default request headers */
  def setDefaultEnv(env: RequestEnv) { defaultEnv = Some(env) }

  type StatusValidator = Envelope.Status => Boolean

  /* --- Thick Interface --- All function prevalidate the response code so the client doesn't have to check it */
  def async_get[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: MultiResult[T] => Unit): Unit = async_verb_wrapper(Envelope.Verb.GET, payload, env, callback)
  def async_delete[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: MultiResult[T] => Unit): Unit = async_verb_wrapper(Envelope.Verb.DELETE, payload, env, callback)
  def async_post[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: MultiResult[T] => Unit): Unit = async_verb_wrapper(Envelope.Verb.POST, payload, env, callback)
  def async_put[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: MultiResult[T] => Unit): Unit = async_verb_wrapper(Envelope.Verb.PUT, payload, env, callback)
  def async_verb[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: RequestEnv = new RequestEnv)(callback: MultiResult[T] => Unit): Unit = async_verb_wrapper(verb, payload, env, callback)

  def async_get_one[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = async_get(payload, env) { checkOne(payload, callback) }
  def async_delete_one[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = async_delete(payload, env) { checkOne(payload, callback) }
  def async_put_one[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv)(callback: SingleResult[T] => Unit): Unit = async_put(payload, env) { checkOne(payload, callback) }

  def get[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv): List[T] = throwFailures(makeCallbackIntoFuture { async_get(payload, env) }())
  def delete[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv): List[T] = throwFailures(makeCallbackIntoFuture { async_delete(payload, env) }())
  def post[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv): List[T] = throwFailures(makeCallbackIntoFuture { async_post(payload, env) }())
  def put[T <: GeneratedMessage](payload: T, env: RequestEnv = new RequestEnv): List[T] = throwFailures(makeCallbackIntoFuture { async_put(payload, env) }())

  def verb[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: RequestEnv = new RequestEnv): List[T] = throwFailures(makeCallbackIntoFuture { async_verb(verb, payload, env) }())

  def async_get_one_scatter[T <: GeneratedMessage](list: List[T])(resp: List[T] => Unit): Unit = async_verb_scatter[T](list, resp, async_get_one[T](_, new RequestEnv))

  private def async_verb_scatter[T <: GeneratedMessage](list: List[T], resp: List[T] => Unit, async_verb: (T) => ((SingleResult[T]) => Unit) => Unit): Unit = {

    // short circuit so we don't do any unnecessary server queries with a list of length 0
    if (list.size == 0) {
      resp(Nil)
      return
    }
    val map = new java.util.concurrent.ConcurrentHashMap[Int, SingleResult[T]]
    val latch = new java.util.concurrent.CountDownLatch(list.size)

    def gather(idx: Int)(value: SingleResult[T]) {
      map.putIfAbsent(idx, value)
      latch.countDown()
      // last callback will pass the results to user code  
      if (latch.getCount == 0) {
        val results = list.indices.map { i =>
          map.get(i).asInstanceOf[SingleResult[T]] match {
            case SingleResponse(x) => x
            case x: Failure => throw x.toException
          }
        }
        // make the final callback to the user code
        resp(results.toList)
      }
    }
    // scatter the requests with the index as the gather id
    list.zipWithIndex.foreach {
      case (proto, i) =>
        // set the callback of the underlying get/put/delete function to our gather function
        async_verb(proto)(gather(i))
    }
  }

  private def checkOne[T <: GeneratedMessage](request: T, callback: SingleResult[T] => Unit): (MultiResult[T]) => Unit = { (multi: MultiResult[T]) =>
    callback(expectOneResponse[T](request, multi))
  }

  private def async_verb_wrapper[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: RequestEnv, callback: MultiResult[T] => Unit) {
    def handleResult(resp: Option[Response[T]]) {
      callback(expect[T](isSuccess, payload, resp))
    }
    request[T](verb, payload, env, handleResult)
  }

  private def expectOneResponse[T <: GeneratedMessage](request: T, response: MultiResult[T]): SingleResult[T] = {
    response match {
      case MultiResponse(List(x)) => SingleResponse(x)
      case MultiResponse(list) =>
        warn { "Unexpected result set size: " + list.size }
        Failure(Envelope.Status.UNEXPECTED_RESPONSE, "Expected one results, but got: " + list.size + " request: " + request)
      case x: Failure => x
    }
  }

  private def expect[T <: GeneratedMessage](validate: StatusValidator, request: T, response: Option[Response[T]]): MultiResult[T] = {
    response match {
      case Some(x) =>
        x match {
          case Response(status, msg, list) =>
            if (validate(status)) MultiResponse(list)
            else {
              warn { "Unsuccessful status from server: " + status + " with message: " + msg + " request: " + request }
              Failure(status, msg)
            }
        }
      case None => Failure(Envelope.Status.RESPONSE_TIMEOUT, "Service response timeout: " + request.getClass().getName() + " " + request)
    }
  }

  private def throwFailures[T <: GeneratedMessage](result: MultiResult[T]): List[T] = {
    result match {
      case MultiResponse(list) => list
      case x: Failure => throw x.toException
    }
  }
  private def throwFailures[T <: GeneratedMessage](result: SingleResult[T]): T = {
    result match {
      case SingleResponse(entry) => entry
      case x: Failure => throw x.toException
    }
  }

  private def makeCallbackIntoFuture[T](fun: (T => Unit) => _): () => T = {
    // http://scala-programming-language.1934581.n4.nabble.com/Scala-Actors-Starvation-td2281657.html
    // http://article.gmane.org/gmane.comp.lang.scala.user/28381/
    // http://stackoverflow.com/questions/954520/does-this-scala-actor-block-when-creating-new-actor-in-a-handler

    val mail = new scala.actors.Channel[T]
    def callback(response: T): Unit = mail ! response

    def wait(): T = {
      //Timing.time("request") {
      mail.receive {
        case x => x.asInstanceOf[T]
      }
      //}
    }
    fun(callback _)
    wait
  }
}