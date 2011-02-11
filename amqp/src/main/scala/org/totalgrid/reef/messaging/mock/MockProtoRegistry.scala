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
import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage

import scala.concurrent.{ MailBox, TIMEOUT }
import scala.collection.immutable

import org.totalgrid.reef.messaging.{ ProtoServiceRegistry, ProtoRegistry }

import org.totalgrid.reef.protoapi.scala.client.ServiceClient
import org.totalgrid.reef.protoapi.{ ServiceTypes, RequestEnv }
import ServiceTypes._

//implicits for massaging service return types
import org.totalgrid.reef.protoapi.scala.client.ProtoConversions._

object MockProtoRegistry {
  val timeout = 5000
}

class MockProtoRegistry extends MockProtoServiceRegistry with MockProtoPublisherRegistry with MockProtoSubscriberRegistry with ProtoRegistry

class MockServiceClient(timeout: Long = MockProtoRegistry.timeout) extends ServiceClient {

  private case class Req[A](callback: MultiResult[A] => Unit, req: Request[A])

  private val in = new MailBox

  def respond[A](f: Request[A] => Option[Response[A]]): Unit = respondWithTimeout(timeout)(f)

  def respondWithTimeout[A](timeout: Long)(f: Request[A] => Option[Response[A]]): Unit = {
    in.receiveWithin(timeout) {
      case Req(callback, request) =>
        callback(f(request.asInstanceOf[Request[A]]))
    }
  }

  def close(): Unit = throw new Exception("Unimplemented")

  def asyncRequest[A](verb: Envelope.Verb, payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit) = {
    in send Req(callback, Request(verb, payload, env))
    Timer.delay(timeout) {
      in.receiveWithin(1) {
        case Req(callback, request) => callback(Failure(Envelope.Status.RESPONSE_TIMEOUT))
        case _ =>
      }
    }
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

trait MockProtoServiceRegistry extends ProtoServiceRegistry {

  val eventmail = new MailBox

  case class EventSub(val klass: Class[_], val mock: MockEvent[_])

  // map classes to protoserviceconsumers
  var mockclient: Option[MockServiceClient] = None
  var eventqueues = immutable.Map.empty[Class[_], Any]

  def getMockClient: MockServiceClient = mockclient.get

  def getServiceClient(key: String): ServiceClient = mockclient match {
    case Some(x) => x
    case None =>
      val ret = new MockServiceClient
      mockclient = Some(ret)
      ret
  }

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

  def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit = {
    eventmail send EventSub(OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]]), MockEvent[A](accept, None))
  }

  def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit = {
    eventmail send EventSub(OneArgFunc.getReturnClass(deserialize, classOf[Array[Byte]]), MockEvent[A](accept, Some(notify)))
  }

}
