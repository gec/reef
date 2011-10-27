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
package org.totalgrid.reef.clientapi.sapi.client.rest.impl

import org.totalgrid.reef.clientapi.exceptions.UnknownServiceException
import org.totalgrid.reef.clientapi.sapi.client.rest.{ Client, RpcProviderInfo, RpcProviderFactory }
import org.totalgrid.reef.clientapi.sapi.types.ServiceInfo

trait DefaultServiceRegistry {

  private var providers = Map.empty[Class[_], RpcProviderFactory]

  private var servicemap = Map.empty[Class[_], ServiceInfo[_, _]]

  def addRpcProvider(info: RpcProviderInfo) = this.synchronized {
    info.interfaces.foreach { i =>
      providers += i -> info.creator
    }
  }

  def getRpcInterface[A](klass: Class[A], client: Client) = this.synchronized {
    providers.get(klass) match {
      case Some(creator) => creator.createRpcProvider(client).asInstanceOf[A]
      case None => throw new UnknownServiceException("Unknown rpc interface for: " + klass)
    }
  }

  def getServiceInfo[A](klass: Class[A]): ServiceInfo[A, _] = servicemap.get(klass) match {
    case Some(info) => info.asInstanceOf[ServiceInfo[A, Any]]
    case None => throw new UnknownServiceException("Unknown service for klass: " + klass)
  }
  def getServiceOption[A](klass: Class[A]): Option[ServiceInfo[A, _]] = servicemap.get(klass).asInstanceOf[Option[ServiceInfo[A, _]]]

  def addServiceInfo[A](info: ServiceInfo[A, _]) = servicemap += info.descriptor.getKlass -> info
}