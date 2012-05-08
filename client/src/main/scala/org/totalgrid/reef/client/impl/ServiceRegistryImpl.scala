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
package org.totalgrid.reef.client.impl

import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.exception.UnknownServiceException
import org.totalgrid.reef.client._

trait ServiceRegistryLookup {
  def buildServiceInterface[A](klass: Class[A], client: => Client): A
  def getServiceOption[A](klass: Class[A]): Option[ServiceTypeInformation[A, _]]
  def getServiceTypeInformation[T](klass: Class[T]): ServiceTypeInformation[T, _]
}

trait ServiceRegistryImpl extends ServiceRegistry with ServiceRegistryLookup {

  def buildServiceInterface[A](klass: Class[A], client: => Client): A = this.synchronized {
    providers.get(klass) match {
      case Some(creator) => creator.createRpcProvider(client).asInstanceOf[A]
      case None => throw new UnknownServiceException("Unknown service interface for: " + klass)
    }
  }

  def getServiceOption[A](klass: Class[A]): Option[ServiceTypeInformation[A, _]] = this.synchronized {
    servicemap.get(klass).asInstanceOf[Option[ServiceTypeInformation[A, _]]]
  }

  private var providers = Map.empty[Class[_], ServiceProviderFactory]
  private var servicemap = Map.empty[Class[_], ServiceTypeInformation[_, _]]

  private def doAddServiceProvider(info: ServiceProviderInfo) {
    import scala.collection.JavaConversions._
    info.getInterfacesImplemented.foreach { i =>
      if (!providers.contains(i)) {
        providers += i -> info.getFactory
      }
    }
  }

  private def doAddServiceInfo[A](info: ServiceTypeInformation[A, _]) {
    val klass = info.getDescriptor.getKlass
    if (!servicemap.contains(klass)) {
      servicemap += klass -> info
    }
  }

  def addServicesList(servicesList: ServicesList) {
    import scala.collection.JavaConversions._
    this.synchronized {
      servicesList.getServiceTypeInformation.foreach { doAddServiceInfo(_) }
      servicesList.getServiceProviders.foreach { doAddServiceProvider(_) }
    }
  }

  def addServiceProvider(info: ServiceProviderInfo) {
    this.synchronized { doAddServiceProvider(info) }
  }

  def addServiceTypeInformation[T, U](typeInformation: ServiceTypeInformation[T, U]) {
    this.synchronized { doAddServiceInfo(typeInformation) }
  }

  def getServiceTypeInformation[T](klass: Class[T]): ServiceTypeInformation[T, _] = this.synchronized {
    servicemap.get(klass) match {
      case Some(info) => info.asInstanceOf[ServiceTypeInformation[T, Any]]
      case None => throw new UnknownServiceException("Unknown service for klass: " + klass + ". Have you added a servicesList?")
    }
  }
}