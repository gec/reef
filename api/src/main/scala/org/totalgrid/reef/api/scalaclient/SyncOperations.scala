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
import ProtoConversions._
import org.totalgrid.reef.api._
import Envelope.Verb._

//implicits

trait SyncOperations {

  self: DefaultHeaders =>

  /**
   * Implement this function to widen the interface
   */
  def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A]

  // helpers
  def requestOne[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): SingleResult[A] = request(verb, payload, env, destination)

  def requestThrow[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): List[A] = request(verb, payload, env, destination)

  def requestOneOrThrow[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): A = requestOne(verb, payload, env, destination)

  def get[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A] = request(GET, payload, env, destination)

  def delete[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A] = request(DELETE, payload, env, destination)

  def post[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A] = request(POST, payload, env, destination)

  def put[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A] = request(PUT, payload, env, destination)

  def getOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): SingleResult[A] = get(payload, env, destination)

  def deleteOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): SingleResult[A] = delete(payload, env, destination)

  def postOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): SingleResult[A] = post(payload, env, destination)

  def putOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): SingleResult[A] = put(payload, env, destination)

  def getOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): List[A] = get(payload, env, destination)

  def deleteOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): List[A] = delete(payload, env, destination)

  def postOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): List[A] = post(payload, env, destination)

  def putOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): List[A] = put(payload, env, destination)

  def getOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): A = getOne(payload, env, destination)

  def deleteOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): A = deleteOne(payload, env, destination)

  def postOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): A = postOne(payload, env, destination)

  def putOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): A = putOne(payload, env, destination)

}