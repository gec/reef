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

import org.totalgrid.reef.protoapi.client.SyncOperations
import org.totalgrid.reef.protoapi.{ RequestEnv, ProtoServiceTypes }
import ProtoServiceTypes.{ Response, MultiResult, Failure }

import org.totalgrid.reef.messaging.{ ServiceRequestHandler, ServicesList }
import org.totalgrid.reef.messaging.javabridge.ProtoDescriptor

import org.osgi.framework.BundleContext
import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.proto.Envelope.Verb

import scala.collection.JavaConversions._
import org.totalgrid.reef.protoapi.ProtoConversions._

class ServiceDispatcher[A <: GeneratedMessage](rh: ServiceRequestHandler, desc: ProtoDescriptor[A]) {

  def request(verb: Verb, payload: A, env: RequestEnv): Response[A] =
    getResponse(rh.respond(getRequest(verb, payload, env), env))

  private def getResponse(rsp: Envelope.ServiceResponse): Response[A] = {
    Response(rsp.getStatus, rsp.getErrorMessage, rsp.getPayloadList.map(x => desc.deserializeString(x)).toList)
  }

  private def getRequest(verb: Envelope.Verb, payload: A, env: RequestEnv): Envelope.ServiceRequest = {
    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId("Console")
    builder.setPayload(payload.toByteString)
    env.asKeyValueList.foreach { x =>
      builder.addHeaders(Envelope.RequestHeader.newBuilder.setKey(x._1).setValue(x._2))
    }
    builder.build
  }

}

trait OSGiSyncOperations extends SyncOperations {

  def getBundleContext: BundleContext

  def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv): MultiResult[A] = ServicesList.getServiceOption(payload.getClass) match {
    case Some(info) =>
      val rsp = new ServiceDispatcher(getService(info.exchange), info.descriptor.asInstanceOf[ProtoDescriptor[A]]).request(verb, payload, env)
      Some(rsp)
    case None =>
      Failure(Envelope.Status.LOCAL_ERROR, "Proto not registered: " + payload.getClass)
  }

  private def getService(exchange: String): ServiceRequestHandler = {
    this.getBundleContext findServices withInterface[ServiceRequestHandler] withFilter "exchange" === exchange andApply { x =>
      x
    } match {
      case Seq(p) => p
      case Seq(_, _) =>
        throw new Exception("Found multiple implementations for service with exchange " + exchange)
      case Nil =>
        throw new Exception("Service unavailble with exchange " + exchange)
    }
  }

}