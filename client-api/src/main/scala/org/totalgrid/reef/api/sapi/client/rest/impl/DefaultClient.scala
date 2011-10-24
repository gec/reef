package org.totalgrid.reef.api.sapi.client.rest.impl

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import org.totalgrid.reef.api.japi.TypeDescriptor
import net.agileautomata.executor4s._
import org.totalgrid.reef.api.sapi.client.rest.{ RpcProviderInfo, Client }
import org.totalgrid.reef.api.sapi.client.{ BasicRequestHeaders, Subscription }
import org.totalgrid.reef.api.japi.Envelope.{ Event, Verb }
import org.totalgrid.reef.api.sapi.service.AsyncService
import org.totalgrid.reef.api.japi.client.Routable
import org.totalgrid.reef.api.sapi.types.ServiceInfo

class DefaultClient(conn: DefaultConnection, strand: Strand) extends Client {

  override def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {
    val usedHeaders = headers.map { getHeaders.merge(_) }.getOrElse(getHeaders)
    conn.request(verb, payload, usedHeaders, strand)
  }

  final override def subscribe[A](descriptor: TypeDescriptor[A]): Result[Subscription[A]] =
    conn.subscribe(strand, descriptor)

  final override def execute(fun: => Unit): Unit = strand.execute(fun)
  final override def attempt[A](fun: => A): Future[Result[A]] = strand.attempt(fun)
  final override def delay(interval: TimeInterval)(fun: => Unit): Cancelable = strand.delay(interval)(fun)

  final override def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) = conn.bindQueueByClass(subQueue, key, klass)
  final override def publishEvent[A](typ: Event, value: A, key: String) = conn.publishEvent(typ, value, key)

  final override def bindService[A](service: AsyncService[A], dispatcher: Executor, destination: Routable, competing: Boolean) = conn.bindService(service, dispatcher, destination, competing)
  final override def declareEventExchange(klass: Class[_]) = conn.declareEventExchange(klass)

  // TODO: clone parent client settings?
  final override def login(authToken: String) = conn.login(authToken)
  final override def login(userName: String, password: String) = conn.login(userName, password)

  final override def addRpcProvider(info: RpcProviderInfo) = conn.addRpcProvider(info)
  final override def getRpcInterface[A](klass: Class[A]) = conn.getRpcInterface(klass, this)

  final override def addServiceInfo[A](info: ServiceInfo[A, _]) = conn.addServiceInfo(info)
}