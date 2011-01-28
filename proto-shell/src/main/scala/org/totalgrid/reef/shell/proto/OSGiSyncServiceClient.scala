/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.messaging.{ ServiceClient, SyncServiceClient }
import org.totalgrid.reef.messaging.{ ServiceRequestHandler, ProtoServiceable, ServicesList, RequestEnv }
import org.totalgrid.reef.messaging.javabridge.ProtoDescriptor
import org.totalgrid.reef.messaging.ProtoServiceTypes.Response

import org.osgi.framework.BundleContext
import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.proto.Envelope.Verb

import scala.collection.JavaConversions._

/*
trait ServiceRequestHandler {

  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse
}
*/

class ServiceDispatcher[A <: GeneratedMessage](rh: ServiceRequestHandler, desc: ProtoDescriptor[A]) {

  def get(payload: A, env: RequestEnv): Response[A] = withVerb(Verb.GET, payload, env)
  def delete(payload: A, env: RequestEnv): Response[A] = withVerb(Verb.DELETE, payload, env)
  def post(payload: A, env: RequestEnv): Response[A] = withVerb(Verb.POST, payload, env)
  def put(payload: A, env: RequestEnv): Response[A] = withVerb(Verb.PUT, payload, env)

  private def withVerb(verb: Verb, payload: A, env: RequestEnv) =
    getResponse(rh.respond(getRequest(verb, payload, env), env))

  private def getResponse(rsp: Envelope.ServiceResponse): Response[A] = {
    Response(rsp.getStatus, rsp.getErrorMessage, rsp.getPayloadList.map(x => desc.deserializeString(x)).toList)
  }

  private def getRequest(verb: Envelope.Verb, payload: A, env: RequestEnv): Envelope.ServiceRequest = {
    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId("Console")
    builder.setPayload(payload.toByteString)
    env.asKeyValueList.foreach { x => builder.addHeaders(Envelope.RequestHeader.newBuilder.setKey(x._1).setValue(x._2)) }
    builder.build
  }

}

trait OSGiSyncServiceClient extends SyncServiceClient {

  def getBundleContext: BundleContext

  def get[A <: GeneratedMessage](req: A, env: RequestEnv = getRequestEnv): List[A] = withService(req.getClass)((x: ServiceDispatcher[A]) => x.get(req, env))
  def put[A <: GeneratedMessage](req: A, env: RequestEnv = getRequestEnv): List[A] = withService(req.getClass)((x: ServiceDispatcher[A]) => x.put(req, env))
  def delete[A <: GeneratedMessage](req: A, env: RequestEnv = getRequestEnv): List[A] = withService(req.getClass)((x: ServiceDispatcher[A]) => x.delete(req, env))
  def post[A <: GeneratedMessage](req: A, env: RequestEnv = getRequestEnv): List[A] = withService(req.getClass)((x: ServiceDispatcher[A]) => x.post(req, env))

  def withService[A <: GeneratedMessage](klass: Class[_])(fun: ServiceDispatcher[A] => Response[A]): List[A] = {
    ServicesList.getServiceOption(klass) match {
      case Some(x) =>
        this.getBundleContext findServices withInterface[ServiceRequestHandler] withFilter "exchange" === x.exchange andApply { (s, _) =>
          new ServiceDispatcher(s, x.descriptor.asInstanceOf[ProtoDescriptor[A]])
        } match {
          case Seq(p) =>
            val rsp = fun(p)
            if (ServiceClient.isSuccess(rsp.status)) rsp.result
            else throw new Exception("Status: " + rsp.status + " Error: " + rsp.error)
          case Seq(p, list) =>
            throw new Exception("Found multiple implementations for " + klass)
          case Nil =>
            throw new Exception("Service unavailble: " + klass)
        }
      case None =>
        throw new Exception("Proto not registered: " + klass)
    }
  }

}