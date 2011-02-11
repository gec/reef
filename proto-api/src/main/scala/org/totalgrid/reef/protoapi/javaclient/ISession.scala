package org.totalgrid.reef.protoapi.javaclient

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
import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.protoapi.{ Envelope, ServiceHandlerHeaders, ISubscription, ITypeDescriptor }
import org.totalgrid.reef.protoapi.ServiceTypes.Event

/**
 *  The interface that a concrete service client must provide.
 */
trait ISession {

  def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: ServiceHandlerHeaders): java.util.List[A]

  def get[A <: AnyRef](payload: A): java.util.List[A]
  def delete[A <: AnyRef](payload: A): java.util.List[A]
  def post[A <: AnyRef](payload: A): java.util.List[A]
  def put[A <: AnyRef](payload: A): java.util.List[A]

  def get[A <: AnyRef](payload: A, sub: ISubscription): java.util.List[A]
  def delete[A <: AnyRef](payload: A, sub: ISubscription): java.util.List[A]
  def post[A <: AnyRef](payload: A, sub: ISubscription): java.util.List[A]
  def put[A <: AnyRef](payload: A, sub: ISubscription): java.util.List[A]

  def getOne[A <: AnyRef](payload: A): A
  def deleteOne[A <: AnyRef](payload: A): A
  def putOne[A <: AnyRef](payload: A): A

  def getOne[A <: AnyRef](payload: A, sub: ISubscription): A
  def deleteOne[A <: AnyRef](payload: A, sub: ISubscription): A
  def putOne[A <: AnyRef](payload: A, sub: ISubscription): A

  def addSubscription[A <: GeneratedMessage](pd: ITypeDescriptor[A], ea: IEventAcceptor[A]): ISubscription

  def getDefaultEnv(): ServiceHandlerHeaders

  def close()
}
