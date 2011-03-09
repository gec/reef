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
package org.totalgrid.reef.api.service.sync

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.api.ServiceTypes.Response
import com.google.protobuf.ByteString

import org.totalgrid.reef.api.{ RequestEnv, Envelope, ReefServiceException }

/**
 *   Implements the ISyncService,respond function in terms of abstract get, put, post, delete
 */
trait SyncServiceBase[A <: AnyRef] extends ISyncService[A] with Logging {

  /* --- Abstract methods --- */

  def get(req: A, env: RequestEnv): Response[A]

  def put(req: A, env: RequestEnv): Response[A]

  def delete(req: A, env: RequestEnv): Response[A]

  def post(req: A, env: RequestEnv): Response[A]

  /* --- Overloaded helpers --- */

  def get(req: A): Response[A] = get(req, new RequestEnv)

  def put(req: A): Response[A] = put(req, new RequestEnv)

  def delete(req: A): Response[A] = delete(req, new RequestEnv)

  def post(req: A): Response[A] = post(req, new RequestEnv)

  /**Generic service handler that we can use to bind the service up to
   * a real messaging system or test
   */
  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {

    val rsp = Envelope.ServiceResponse.newBuilder.setId(req.getId)

    def setRsp(r: Response[A]) = {
      rsp.setStatus(r.status).setErrorMessage(r.error)
      r.result.foreach { x: A => rsp.addPayload(ByteString.copyFrom(descriptor.serialize(x))) }
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

  /** by default, unimplemented verbs return this response */
  protected def noVerb(verb: String) = Response[A](Envelope.Status.NOT_ALLOWED, "Unimplemented verb: " + verb, Nil)

  private def handleRequest(request: Envelope.ServiceRequest, env: RequestEnv): Response[A] = {
    val value = descriptor.deserialize(request.getPayload.toByteArray)
    request.getVerb match {
      case Envelope.Verb.GET => get(value, env)
      case Envelope.Verb.PUT => put(value, env)
      case Envelope.Verb.DELETE => delete(value, env)
      case Envelope.Verb.POST => post(value, env)
    }
  }

}