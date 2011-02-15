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
package org.totalgrid.reef.api

class UnknownServiceException(msg: String) extends Exception(msg)

object ServiceList {

  type ServiceMap = Map[Class[_], ServiceInfo[_, _]]
  type ServiceTuple = Tuple2[Class[_], ServiceInfo[_, _]]

  def getServiceInfo[A](klass: Class[A], map: ServiceMap): ServiceInfo[A, _] = map.get(klass) match {
    case Some(info) => info.asInstanceOf[ServiceInfo[A, Any]]
    case None => throw new UnknownServiceException("Unknown service for klass: " + klass)
  }
}

trait ServiceList {
  def getServiceInfo[A](klass: Class[A]): ServiceInfo[A, _]
  def getServiceOption[A](klass: Class[A]): Option[ServiceInfo[A, _]]
}

class ServiceListOnMap(servicemap: ServiceList.ServiceMap) extends ServiceList {
  def getServiceInfo[A](klass: Class[A]): ServiceInfo[A, _] = ServiceList.getServiceInfo(klass, servicemap)
  def getServiceOption[A](klass: Class[A]): Option[ServiceInfo[A, _]] = servicemap.get(klass).asInstanceOf[Option[ServiceInfo[A, _]]]
}

