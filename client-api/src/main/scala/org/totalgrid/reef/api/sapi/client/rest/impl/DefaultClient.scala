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
import org.totalgrid.reef.api.sapi.client.rest.{ RestOperations, Client }
import org.totalgrid.reef.api.japi.Envelope.Verb
import org.totalgrid.reef.api.sapi.client.{ BasicRequestHeaders, Subscription }

final class DefaultClient(conn: DefaultConnection, strand: Strand) extends Client with RestOperations {

  override def request[A](verb: Verb, payload: A, headers: BasicRequestHeaders = getHeaders) =
    conn.request(verb, payload, getHeaders.merge(headers), strand)

  override def subscribe[A](descriptor: TypeDescriptor[A]): Result[Subscription[A]] =
    conn.subscribe(strand, descriptor.getKlass)

  override def execute(fun: => Unit): Unit = strand.execute(fun)
  override def attempt[A](fun: => A): Future[Result[A]] = strand.attempt(fun)
  override def delay(interval: TimeInterval)(fun: => Unit): Cancelable = strand.delay(interval)(fun)

}