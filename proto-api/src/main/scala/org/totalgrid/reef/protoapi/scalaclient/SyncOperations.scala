package org.totalgrid.reef.protoapi.scalaclient

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
import org.totalgrid.reef.protoapi._
import ServiceTypes._
import Envelope.Verb._

//implicits

trait SyncOperations {

  self: DefaultHeaders =>

  /**
   * Implement this function to widen the interface
   */
  def request[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders): MultiResult[A]

  // helpers
  def requestOne[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders): SingleResult[A] = request(verb, payload, env)

  def requestThrow[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders): List[A] = request(verb, payload, env)

  def requestOneOrThrow[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders): A = requestOne(verb, payload, env)

  def get[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): MultiResult[A] = request(GET, payload, env)

  def delete[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): MultiResult[A] = request(DELETE, payload, env)

  def post[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): MultiResult[A] = request(POST, payload, env)

  def put[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): MultiResult[A] = request(PUT, payload, env)

  def getOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): SingleResult[A] = get(payload, env)

  def deleteOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): SingleResult[A] = delete(payload, env)

  def postOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): SingleResult[A] = post(payload, env)

  def putOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): SingleResult[A] = put(payload, env)

  def getOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): List[A] = get(payload, env)

  def deleteOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): List[A] = delete(payload, env)

  def postOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): List[A] = post(payload, env)

  def putOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): List[A] = put(payload, env)

  def getOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): A = getOne(payload, env)

  def deleteOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): A = deleteOne(payload, env)

  def postOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): A = postOne(payload, env)

  def putOneOrThrow[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders): A = putOne(payload, env)

}