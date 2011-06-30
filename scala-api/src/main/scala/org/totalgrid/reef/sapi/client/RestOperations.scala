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
package org.totalgrid.reef.sapi.client

import org.totalgrid.reef.sapi._
import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.promise.Promise

trait RestOperations {

  self: DefaultHeaders =>

  def request[A](verb: Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination): Promise[Response[A]]

  final def get[A](payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination) = request(Verb.GET, payload, env, destination)
  final def delete[A](payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination) = request(Verb.DELETE, payload, env, destination)
  final def post[A](payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination) = request(Verb.POST, payload, env, destination)
  final def put[A](payload: A, env: RequestEnv = getDefaultHeaders, destination: Destination = AnyNodeDestination) = request(Verb.PUT, payload, env, destination)

}