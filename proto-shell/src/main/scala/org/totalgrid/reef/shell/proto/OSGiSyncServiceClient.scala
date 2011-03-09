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

import org.totalgrid.reef.api.scalaclient.{ SyncOperations, DefaultHeaders }
import org.totalgrid.reef.api.ServiceTypes.{ Response, MultiResult, Failure }
import org.totalgrid.reef.api.service.sync.ServiceDescriptor

import org.totalgrid.reef.api.Envelope.Verb
import org.totalgrid.reef.proto.ReefServicesList
import org.osgi.framework.BundleContext
import com.weiglewilczek.scalamodules._

import _root_.scala.collection.JavaConversions._
import org.totalgrid.reef.api.scalaclient.ProtoConversions._
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.api._

class ServiceDispatcher[A <: AnyRef](rh: ServiceDescriptor[A]) {

  def request(verb: Verb, payload: A, env: RequestEnv): Response[A] =
    getResponse(rh.respond(getRequest(verb, payload, env), env))

  private def getResponse(rsp: Envelope.ServiceResponse): Response[A] = {
    Response(rsp.getStatus, rsp.getErrorMessage, rsp.getPayloadList.map(x => rh.descriptor.deserialize(x.toByteArray)).toList)
  }

  private def getRequest(verb: Envelope.Verb, payload: A, env: RequestEnv): Envelope.ServiceRequest = {
    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId("Console")
    builder.setPayload(rh.descriptor.serialize(payload))
    env.asKeyValueList.foreach { x =>
      builder.addHeaders(Envelope.RequestHeader.newBuilder.setKey(x._1).setValue(x._2))
    }
    builder.build
  }

}

trait OSGiSyncOperations extends SyncOperations with DefaultHeaders {

  def getBundleContext: BundleContext

  def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv): MultiResult[A] = ReefServicesList.getServiceOption(payload.getClass) match {
    case Some(info) =>
      val rsp = new ServiceDispatcher[A](getService[A](info.exchange)).request(verb, payload, env)
      Some(rsp)
    case None =>
      Failure(Envelope.Status.LOCAL_ERROR, "Proto not registered: " + payload.getClass)
  }

  private def getService[A](exchange: String): ServiceDescriptor[A] = {
    this.getBundleContext findServices withInterface[ServiceDescriptor[_]] withFilter "exchange" === exchange andApply { x =>
      x.asInstanceOf[ServiceDescriptor[A]]
    } match {
      case Seq(p) => p
      case Seq(_, _) =>
        throw new Exception("Found multiple implementations for service with exchange " + exchange)
      case Nil =>
        throw new Exception("Service unavailble with exchange " + exchange)
    }
  }

}