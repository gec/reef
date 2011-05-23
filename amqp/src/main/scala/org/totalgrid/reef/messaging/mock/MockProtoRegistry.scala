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
package org.totalgrid.reef.messaging.mock

import org.totalgrid.reef.util.{ Timer, OneArgFunc }

import com.google.protobuf.GeneratedMessage

import scala.concurrent.MailBox
import scala.collection.immutable

import org.totalgrid.reef.api.service.IServiceAsync

import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.api.{ Envelope, RequestEnv, IDestination }

import org.totalgrid.reef.api.scalaclient._
import org.totalgrid.reef.messaging.Connection

//implicits for massaging service return types

object MockProtoRegistry {
  val timeout = 5000
}

class MockRegistry extends MockConnection with MockProtoPublisherRegistry with MockProtoSubscriberRegistry

class MockClientSession(timeout: Long = MockProtoRegistry.timeout) extends ClientSession with AsyncRestAdapter {

  private case class Req[A](callback: Response[A] => Unit, req: Request[A])

  private val in = new MailBox

  def respond[A](f: Request[A] => Option[Response[A]]): Unit = respondWithTimeout(timeout)(f)

  def respondWithTimeout[A](timeout: Long)(f: Request[A] => Option[Response[A]]): Unit = {
    in.receiveWithin(timeout) {
      case Req(callback, request) =>
        callback(Response.convert(f(request.asInstanceOf[Request[A]])))
    }
  }

  def close(): Unit = throw new Exception("Unimplemented")

  def asyncRequest[A](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: IDestination)(callback: Response[A] => Unit) = {
    in send Req(callback, Request(verb, payload, env))
    Timer.delay(timeout) {
      in.receiveWithin(1) {
        case Req(callback, request) => callback(ResponseTimeout)
        case _ =>
      }
    }
  }

  final override def addSubscription[A](klass: Class[_]) = {
    throw new IllegalArgumentException("Subscriptions not implemented for MockRegistry.")
  }
}

trait MockProtoPublisherRegistry {

  private var pubmap = immutable.Map.empty[Tuple2[Class[_], String], MailBox]

  def publish[A <: GeneratedMessage](keygen: A => String, hint: String = ""): A => Unit = {
    val klass = OneArgFunc.getParamClass(keygen, classOf[String])
    pubmap.get((klass, hint)) match {
      case Some(x) => {
        x send _
      }
      case None =>
        pubmap += (klass, hint) -> new MailBox
        publish(keygen)
    }
  }

  def broadcast[A <: GeneratedMessage](exchangeName: String, keygen: A => String): A => Unit = {
    val klass = OneArgFunc.getParamClass(keygen, classOf[String])
    pubmap.get((klass, exchangeName)) match {
      case Some(x) => {
        x send _
      }
      case None =>
        pubmap += (klass, exchangeName) -> new MailBox
        publish(keygen)
    }
  }

  def getMailbox[A <: GeneratedMessage](klass: Class[A]): MailBox = getMailbox(klass, "")

  def getMailbox[A <: GeneratedMessage](klass: Class[A], hint: String): MailBox = pubmap(klass, hint)

}

trait MockProtoSubscriberRegistry {

  private val mail = new MailBox

  private var submap = immutable.Map.empty[Tuple2[Class[_], String], Any]

  def getAcceptor[A](klass: Class[A]): A => Unit = getAcceptor(klass, "", MockProtoRegistry.timeout)

  def getAcceptor[A](klass: Class[A], timeout: Long): A => Unit = getAcceptor(klass, "", timeout)

  def getAcceptor[A](klass: Class[A], hint: String, timeout: Long): A => Unit = {
    if (timeout > 0) {
      submap.get(klass, hint) match {
        case None =>
          mail.receiveWithin(timeout) {
            case "subscribe" => getAcceptor(klass, hint, timeout)
          }
        case _ =>
      }
    }

    submap(klass, hint).asInstanceOf[A => Unit]

  }

  def subscribe[A](deserialize: Array[Byte] => A, key: String, hint: String = "")(accept: A => Unit): Unit = {
    val klass = OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]])
    submap.get(klass, hint) match {
      case Some(x) =>
      case None =>
        submap += (klass, hint) -> accept
        mail send "subscribe"
    }
  }

  def listen[A](deserialize: (Array[Byte]) => A, queueName: String)(accept: A => Unit): Unit = {
    val klass = OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]])
    submap.get(klass, queueName) match {
      case Some(x) =>
      case None =>
        submap += (klass, queueName) -> accept
    }
  }

}

case class MockEvent[A](accept: Event[A] => Unit, observer: Option[String => Unit])

class MockConnection extends Connection {

  val eventmail = new MailBox
  val servicemail = new MailBox

  case class EventSub(val klass: Class[_], val mock: MockEvent[_])
  case class ServiceBinding(service: IServiceAsync[_], destination: IDestination, competing: Boolean, reactor: Option[Reactable])

  // map classes to protoserviceconsumers
  var mockclient: Option[MockClientSession] = None

  var eventqueues = immutable.Map.empty[Class[_], Any]

  def getMockClient: MockClientSession = mockclient.get

  def getEvent[A](klass: Class[A]): MockEvent[A] = {
    eventqueues.get(klass) match {
      case Some(x) => x.asInstanceOf[MockEvent[A]]
      case None =>
        eventmail.receiveWithin(MockProtoRegistry.timeout) {
          case EventSub(k, m) =>
            eventqueues += (k -> m)
            getEvent(klass)
        }
    }
  }

  private lazy val pool = {
    val session = new MockClientSession
    mockclient = Some(session)
    new MockSessionPool(session)
  }

  final override def getSessionPool() = pool

  override def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit = {
    eventmail send EventSub(OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]]), MockEvent[A](accept, None))
  }

  override def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit = {
    eventmail send EventSub(OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]]), MockEvent[A](accept, Some(notify)))
  }

  override def bindService(service: IServiceAsync[_], destination: IDestination, competing: Boolean, reactor: Option[Reactable]): Unit = {
    servicemail send ServiceBinding(service, destination, competing, reactor)
  }

}
