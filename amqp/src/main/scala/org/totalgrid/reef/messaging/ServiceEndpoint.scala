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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.api._

//import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.messaging.ProtoSerializer._

import org.totalgrid.reef.api.ServiceTypes.Response

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
  val useAuth = true
}

trait ServiceDescriptor[A] extends ServiceRequestHandler {
  val descriptor: ITypeDescriptor[A]
}

trait ServiceEndpoint[A <: AnyRef] extends ServiceDescriptor[A] with Logging {

  def get(req: A): Response[A] = get(req, new RequestEnv)
  def put(req: A): Response[A] = put(req, new RequestEnv)
  def delete(req: A): Response[A] = delete(req, new RequestEnv)
  def post(req: A): Response[A] = post(req, new RequestEnv)

  def get(req: A, env: RequestEnv): Response[A]
  def put(req: A, env: RequestEnv): Response[A]
  def delete(req: A, env: RequestEnv): Response[A]
  def post(req: A, env: RequestEnv): Response[A]

  /// by default, unimplemented verbs return this response
  protected def noVerb(verb: String) = Response[A](Envelope.Status.NOT_ALLOWED, "Unimplemented verb: " + verb, Nil)

  def handleRequest(verb: Envelope.Verb, value: A, env: RequestEnv): Response[A] = {
    verb match {
      case Envelope.Verb.GET => get(value, env)
      case Envelope.Verb.PUT => put(value, env)
      case Envelope.Verb.DELETE => delete(value, env)
      case Envelope.Verb.POST => post(value, env)
      case _ => noVerb(verb.toString)
    }
  }

  def handleRequest(req: Envelope.ServiceRequest, env: RequestEnv): Response[A] = {

    val value = descriptor.deserialize(req.getPayload.toByteArray)
    handleRequest(req.getVerb, value, env)
  }

  /** Generic service handler that we can use to bind the service up to
   * a real messaging system or test
   */
  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {

    val rsp = Envelope.ServiceResponse.newBuilder.setId(req.getId)

    def setRsp(r: Response[A]) = {
      rsp.setStatus(r.status).setErrorMessage(r.error)
      r.result.foreach { x: A => rsp.addPayload(descriptor.serialize(x)) }
    }

    try {
      setRsp(handleRequest(req, env))
    } catch {
      case px: ReefServiceException =>
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

