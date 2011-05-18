package org.totalgrid.reef.api.javaclient;

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

import org.totalgrid.reef.api.*;

/**
 *  The interface that a concrete service client must provide.
 */
public interface ISession {

  /* -------- Synchronous API ------------ */

  <A> IPromise<IResponse<A>> get(A payload) throws ReefServiceException;
  <A> IPromise<IResponse<A>> delete(A payload) throws ReefServiceException;
  <A> IPromise<IResponse<A>> post(A payload) throws ReefServiceException;
  <A> IPromise<IResponse<A>> put(A payload) throws ReefServiceException;


  <A> IPromise<IResponse<A>> get(A payload, ISubscription<A> subscription) throws ReefServiceException;
  <A> IPromise<IResponse<A>> delete(A payload, ISubscription<A> subscription) throws ReefServiceException;
  <A> IPromise<IResponse<A>> post(A payload, ISubscription<A> subscription) throws ReefServiceException;
  <A> IPromise<IResponse<A>> put(A payload, ISubscription<A> subscription) throws ReefServiceException;

  /* --- Misc --- */

  <A> ISubscription<A> addSubscription(ITypeDescriptor<A> descriptor) throws ServiceIOException;

  <A> ISubscription<A> addSubscription(ITypeDescriptor<A> descriptor, IEventAcceptor<A> acceptor) throws ServiceIOException;

  IServiceHeaders getDefaultHeaders();

  void close();

}
