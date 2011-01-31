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
package org.totalgrid.reef.messaging.javabridge

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.messaging.ServiceHandlerHeaders

/**
 * Non-Thread-Safe factory class to provide service clients to a single thread. The clients
 * produced by this factory all use the same underlying channel (Session in qpid parlance)
 * and using them from multiple threads _will_ cause failures. The clients themselves are very
 * lightweight and could be cached for memory efficiency. They should be thrown away 
 * after use. When the factory is closed all future requests on the service clients 
 * will fail instantly.
 */
trait IJavaServiceClientFactory {
  /**
   * gets a reference to the mutable header structure shared by all clients produced by this
   * bridge. Useful for auth_tokens or other log lived data that needs to be attached to all
   * requests. If auth tokens are attached that would mean we would need to have one bridge 
   * per user, only feasible during testing. These headers are merged with the per-action envs.
   */
  def getDefaultEnv(): ServiceHandlerHeaders

  /**
   *  Returns a JavaServiceClient for interacting with a particular resource type
   */
  def getServiceClient[T <: GeneratedMessage](pd: ProtoDescriptor[T]): IServiceClient

  /**
   * closes the client factory and the underlying channel, all serviceClients 
   */
  def close(): Unit
}