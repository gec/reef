/**
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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.messaging.ProtoServiceTypes._

class ProtoServiceException(val msg: String, val status: Envelope.Status = Envelope.Status.BAD_REQUEST) extends RuntimeException(msg) {
  def getStatus = status
  def getMsg = msg
}

object ServiceRequestHandler {
  /**
   * type of ServiceRequestHandler.respond
   */
  type Respond = (Envelope.ServiceRequest, RequestEnv) => Envelope.ServiceResponse
}
/**
 * classes that are going to be handling service requests should inherit this interface
 * to provide a consistent interface so we can easily implement a type of "middleware" 
 * wrapper that layers functionality on a request 
 */
trait ServiceRequestHandler {

  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse
}

trait ProtoServiceable[T <: GeneratedMessage] extends ServiceRequestHandler with Logging {

  /// Implement this member to tell the trait how to read it's type
  def deserialize(bytes: Array[Byte]): T

  def get(req: T): Response[T] = get(req, new RequestEnv)
  def put(req: T): Response[T] = put(req, new RequestEnv)
  def delete(req: T): Response[T] = delete(req, new RequestEnv)
  def post(req: T): Response[T] = post(req, new RequestEnv)

  def get(req: T, env: RequestEnv): Response[T]
  def put(req: T, env: RequestEnv): Response[T]
  def delete(req: T, env: RequestEnv): Response[T]
  def post(req: T, env: RequestEnv): Response[T]

  /// by default, unimplemented verbs return this response
  protected def noVerb(verb: String) = Response[T](Envelope.Status.NOT_ALLOWED, "Unimplemented verb: " + verb, Nil)

  def handleRequest(verb: Envelope.Verb, value: T, env: RequestEnv): Response[T] = {
    verb match {
      case Envelope.Verb.GET => get(value, env)
      case Envelope.Verb.PUT => put(value, env)
      case Envelope.Verb.DELETE => delete(value, env)
      case Envelope.Verb.POST => post(value, env)
      case _ => noVerb(verb.toString)
    }
  }

  def handleRequest(req: Envelope.ServiceRequest, env: RequestEnv): Response[T] = {

    val value = deserialize(req.getPayload.toByteArray)
    handleRequest(req.getVerb, value, env)
  }

  /** Generic service handler that we can use to bind the service up to
   * a real messaging system or test
   */
  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {

    val rsp = Envelope.ServiceResponse.newBuilder.setId(req.getId)

    def setRsp(r: Response[T]) = {
      rsp.setStatus(r.status).setErrorMessage(r.error)
      r.result.foreach { x: T => rsp.addPayload(x.toByteString) }
    }

    try {
      setRsp(handleRequest(req, env))
    } catch {
      case px: ProtoServiceException =>
        error(px)
        rsp.setStatus(px.status).setErrorMessage(px.toString)
      case x: Exception =>
        error(x)
        val result = new java.io.StringWriter()
        val printWriter = new java.io.PrintWriter(result)
        x.printStackTrace(printWriter)
        val msg = x.toString + "\n" + result.toString
        rsp.setStatus(Envelope.Status.BAD_REQUEST).setErrorMessage(msg)
    }
    rsp.build
  }

}

