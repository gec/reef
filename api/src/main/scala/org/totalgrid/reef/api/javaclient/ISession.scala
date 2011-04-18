package org.totalgrid.reef.api.javaclient

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

import org.totalgrid.reef.api.{ Envelope, ServiceHandlerHeaders, ReefServiceException, ServiceIOException, ISubscription, IHeaderInfo, ITypeDescriptor }
import org.totalgrid.reef.api.scalaclient.ClientSession

/**
 *  The interface that a concrete service client must provide.
 */
trait ISession {

  @throws(classOf[ReefServiceException])
  def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: ServiceHandlerHeaders): java.util.List[A]

  /* -------- Synchronous API ------------ */

  @throws(classOf[ReefServiceException])
  def get[A <: AnyRef](payload: A): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def delete[A <: AnyRef](payload: A): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def post[A <: AnyRef](payload: A): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def put[A <: AnyRef](payload: A): java.util.List[A]

  @throws(classOf[ReefServiceException])
  def get[A <: AnyRef](payload: A, hdr: IHeaderInfo): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def delete[A <: AnyRef](payload: A, hdr: IHeaderInfo): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def post[A <: AnyRef](payload: A, hdr: IHeaderInfo): java.util.List[A]
  @throws(classOf[ReefServiceException])
  def put[A <: AnyRef](payload: A, hdr: IHeaderInfo): java.util.List[A]

  @throws(classOf[ReefServiceException])
  def getOne[A <: AnyRef](payload: A): A
  @throws(classOf[ReefServiceException])
  def deleteOne[A <: AnyRef](payload: A): A
  @throws(classOf[ReefServiceException])
  def postOne[A <: AnyRef](payload: A): A
  @throws(classOf[ReefServiceException])
  def putOne[A <: AnyRef](payload: A): A

  @throws(classOf[ReefServiceException])
  def getOne[A <: AnyRef](payload: A, hdr: IHeaderInfo): A
  @throws(classOf[ReefServiceException])
  def deleteOne[A <: AnyRef](payload: A, hdr: IHeaderInfo): A
  @throws(classOf[ReefServiceException])
  def postOne[A <: AnyRef](payload: A, hdr: IHeaderInfo): A
  @throws(classOf[ReefServiceException])
  def putOne[A <: AnyRef](payload: A, hdr: IHeaderInfo): A

  /* -------- Future API ------------ */
  @throws(classOf[ServiceIOException])
  def getFuture[A <: AnyRef](payload: A): IFuture[A]
  @throws(classOf[ServiceIOException])
  def deleteFuture[A <: AnyRef](payload: A): IFuture[A]
  @throws(classOf[ServiceIOException])
  def postFuture[A <: AnyRef](payload: A): IFuture[A]
  @throws(classOf[ServiceIOException])
  def putFuture[A <: AnyRef](payload: A): IFuture[A]

  @throws(classOf[ServiceIOException])
  def getFuture[A <: AnyRef](payload: A, hdr: IHeaderInfo): IFuture[A]
  @throws(classOf[ServiceIOException])
  def deleteFuture[A <: AnyRef](payload: A, hdr: IHeaderInfo): IFuture[A]
  @throws(classOf[ServiceIOException])
  def postFuture[A <: AnyRef](payload: A, hdr: IHeaderInfo): IFuture[A]
  @throws(classOf[ServiceIOException])
  def putFuture[A <: AnyRef](payload: A, hdr: IHeaderInfo): IFuture[A]

  /* -------- Asynchronous API ------ */
  @throws(classOf[ServiceIOException])
  def getAsync[A <: AnyRef](payload: A, callback: IResultAcceptor[A])
  @throws(classOf[ServiceIOException])
  def deleteAsync[A <: AnyRef](payload: A, callback: IResultAcceptor[A])
  @throws(classOf[ServiceIOException])
  def postAsync[A <: AnyRef](payload: A, callback: IResultAcceptor[A])
  @throws(classOf[ServiceIOException])
  def putAsync[A <: AnyRef](payload: A, callback: IResultAcceptor[A])

  /* --- Misc --- */
  @throws(classOf[ServiceIOException])
  def addSubscription[A <: GeneratedMessage](descriptor: ITypeDescriptor[A], callback: IEventAcceptor[A]): ISubscription[A]

  def getDefaultEnv(): ServiceHandlerHeaders

  def close()

  def getUnderlyingClient(): ClientSession
}
