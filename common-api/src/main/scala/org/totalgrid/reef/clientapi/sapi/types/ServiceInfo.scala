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
package org.totalgrid.reef.clientapi.sapi.types

import org.totalgrid.reef.clientapi.types.TypeDescriptor

// TODO: make ServiceInfo a sibling of TypeDescriptor
object ServiceInfo {
  private def getEventExchange[A](desc: TypeDescriptor[A]) = desc.id + "_events"

  def apply[A](descriptor: TypeDescriptor[A]): ServiceInfo[A, A] =
    ServiceInfo[A, A](descriptor, descriptor, getEventExchange(descriptor))

  def apply[A, B](descriptor: TypeDescriptor[A], subDescriptor: TypeDescriptor[B]): ServiceInfo[A, B] =
    ServiceInfo[A, B](descriptor, subDescriptor, getEventExchange(subDescriptor))

}

case class ServiceInfo[A, B](descriptor: TypeDescriptor[A], subType: TypeDescriptor[B], subExchange: String)

