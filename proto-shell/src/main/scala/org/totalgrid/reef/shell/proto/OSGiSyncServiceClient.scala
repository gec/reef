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

import org.totalgrid.reef.api.service.{ IServiceAsync, IServiceResponseCallback }

import org.totalgrid.reef.api.Envelope.Verb
import org.totalgrid.reef.proto.ReefServicesList
import org.osgi.framework.BundleContext
import com.weiglewilczek.scalamodules._

import scala.annotation.tailrec

import _root_.scala.collection.JavaConversions._
import org.totalgrid.reef.api.scalaclient.ProtoConversions._
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.api._
import scalaclient.{ SyncClientSession, SyncOperations, DefaultHeaders }
import org.totalgrid.reef.api.ServiceTypes.{ Event, Response, MultiResult, Failure }

class ServiceDispatcher[A <: AnyRef](rh: IServiceAsync[A]) {

  def request(verb: Verb, payload: A, env: RequestEnv, timeoutms: Long = 5000): Response[A] = {

    val mutex = new Object
    var ret: Option[Envelope.ServiceResponse] = None

    val callback = new IServiceResponseCallback {
      def onResponse(rsp: Envelope.ServiceResponse): Unit = mutex.synchronized {
        ret = Some(rsp)
        mutex.notify()
      }
    }

    def extract: Envelope.ServiceResponse = mutex.synchronized {
      @tailrec
      def extract(wait: Boolean): Envelope.ServiceResponse = ret match {
        case Some(x) => x
        case None =>
          if (wait) {
            mutex.wait(timeoutms)
            extract(false)
          } else throw new ResponseTimeoutException
      }
      extract(true)
    }

    rh.respond(getRequest(verb, payload, env), env, callback)
    getResponse(extract)
  }

  private def getResponse(rsp: Envelope.ServiceResponse): Response[A] = {
    val result = rsp.getPayloadList.map(x => rh.descriptor.deserialize(x.toByteArray)).toList
    Response(rsp.getStatus, result, rsp.getErrorMessage)
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

  override def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: IDestination = AnyNode): MultiResult[A] = ReefServicesList.getServiceOption(payload.getClass) match {
    case Some(info) =>
      val rsp = new ServiceDispatcher[A](getService[A](info.exchange)).request(verb, payload, env.merge(new RequestEnv))
      Some(rsp)
    case None =>
      Failure(Envelope.Status.LOCAL_ERROR, "Proto not registered: " + payload.getClass)
  }

  private def getService[A <: AnyRef](exchange: String): IServiceAsync[A] = {
    this.getBundleContext findServices withInterface[IServiceAsync[_]] withFilter "exchange" === exchange andApply { x =>
      x.asInstanceOf[IServiceAsync[A]]
    } match {
      case Seq(p) => p
      case Seq(_, _) =>
        throw new Exception("Found multiple implementations for service with exchange " + exchange)
      case Nil =>
        throw new Exception("Service unavailble with exchange " + exchange)
    }
  }

}

class OSGISession(bundleContext: BundleContext) extends OSGiSyncOperations with SyncClientSession {
  def getBundleContext: BundleContext = bundleContext

  def addSubscription[A <: GeneratedMessage](klass: Class[_]) = {
    throw new IllegalArgumentException("Subscriptions not implemented for OSGISession.")
  }

  def close() {
    // nothing special to do
  }
}