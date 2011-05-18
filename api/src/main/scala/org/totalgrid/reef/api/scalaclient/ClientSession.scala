package org.totalgrid.reef.api.scalaclient

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
import org.totalgrid.reef.api.{ Envelope, RequestEnv, IDestination }

/**
 * scala analog to the java ISession
 */
trait ClientSession extends SyncClientSession with ClientOperations

/**
 * Simplest ClientSession that supports only synchronous requests
 */
trait SyncClientSession extends SyncOperations with SubscriptionManagement with Closeable with DefaultHeaders

/**
 * all clients should be closeable, this interface makes that explict
 */
trait Closeable {
  /**
   * clients should be closed before being thrown away
   */
  def close()
}

/**
 * Provides a thick interface full of helper functions via implement of a single abstract request function
 */
trait ClientOperations
    extends SyncOperations
    with AsyncOperations
    with DefaultHeaders {

  /**
   *    Return a promise.
   */
  override def request[A](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: IDestination): IPromise[Response[A]] = {
    val promise = new Promise[Response[A]]
    this.asyncRequest(verb, payload, env, dest)(promise.onResponse(_))
    promise
  }

}