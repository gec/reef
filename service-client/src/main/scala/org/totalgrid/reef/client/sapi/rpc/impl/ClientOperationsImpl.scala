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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.sapi.rpc.ClientOperations
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations

trait ClientOperationsImpl extends HasAnnotatedOperations with ClientOperations {
  def getOne[T](request: T) = ops.operation("Cannot getOne with request: " + request) {
    _.get(request).map(_.one)
  }

  def findOne[T](request: T) = ops.operation("Cannot findOne with request: " + request) {
    _.get(request).map(_.oneOrNone)
  }

  def getMany[T](request: T) = ops.operation("Cannot getMany with request: " + request) {
    _.get(request).map(_.many)
  }

  def subscribeMany[T](request: T) = {
    import org.totalgrid.reef.client.types.TypeDescriptor
    import org.totalgrid.reef.client.sapi.client.Subscription._

    val descriptor = serviceRegistry.getServiceInfo(request.asInstanceOf[AnyRef].getClass).getDescriptor
    val typeDescriptor = descriptor.asInstanceOf[TypeDescriptor[T]]
    ops.subscription(typeDescriptor, "Cannot getMany with request: " + request) { (sub, c) =>
      c.get(request, sub).map(_.many)
    }
  }

  def deleteOne[T](request: T) = ops.operation("Cannot deleteOne with request: " + request) {
    _.delete(request).map(_.one)
  }

  def deleteMany[T](request: T) = ops.operation("Cannot deleteMany with request: " + request) {
    _.delete(request).map(_.many)
  }

  def putOne[T](request: T) = ops.operation("Cannot putOne with request: " + request) {
    _.put(request).map(_.one)
  }

  def putMany[T](request: T) = ops.operation("Cannot putMany with request: " + request) {
    _.put(request).map(_.many)
  }

  def postOne[T](request: T) = ops.operation("Cannot postOne with request: " + request) {
    _.post(request).map(_.one)
  }

  def postMany[T](request: T) = ops.operation("Cannot postMany with request: " + request) {
    _.post(request).map(_.many)
  }
}