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
package org.totalgrid.reef.protocol.api

import org.totalgrid.reef.proto.{ FEP, Model }
import scala.collection.immutable

import org.totalgrid.reef.util.Logging

trait BaseProtocol extends IProtocol with Logging {

  case class Endpoint(name: String, port: Option[FEP.Port], config: List[Model.ConfigFile]) /// The issue function and the port

  // only mutable state is current assignment of these variables
  private var endpoints = immutable.Map.empty[String, Endpoint] /// maps uids to a Endpoint
  private var ports = immutable.Map.empty[String, FEP.Port] /// maps uids to a Port

  def addPort(p: FEP.Port): Unit = {
    ports.get(p.getName) match {
      case None =>
        ports = ports + (p.getName -> p)
        _addPort(p)
      case Some(x) =>
        if (x == p) info("Ignoring duplicate port " + p)
        else throw new IllegalArgumentException("Port with that name already exists: " + p)
    }
  }

  def addEndpoint(endpoint: String, portName: String, config: List[Model.ConfigFile], publish: IProtocol.Publish, command: IProtocol.Respond): IProtocol.Issue = {

    endpoints.get(endpoint) match {
      case Some(x) => throw new IllegalArgumentException("Endpoint already exists: " + endpoint)
      case None =>
        ports.get(portName) match {
          case Some(p) =>
            endpoints += endpoint -> Endpoint(endpoint, Some(p), config)
            _addEndpoint(endpoint, portName, config, publish, command)
          case None =>
            if (requiresPort) throw new IllegalArgumentException("Port not registered " + portName)
            endpoints += endpoint -> Endpoint(endpoint, None, config)
            _addEndpoint(endpoint, portName, config, publish, command)
        }
    }
  }

  def removePort(port: String): Unit = {
    ports.get(port) match {
      case Some(p) =>
        endpoints.values.filter { e => // if a port is removed, remove all devices on that port first
          e.port match {
            case Some(x) => x.getName == port
            case None => false
          }
        }.foreach { e => removeEndpoint(e.name) }
        ports -= port
        _removePort(port)
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown port " + port)
    }
  }

  /// remove the device from the map and its port's device list
  def removeEndpoint(endpoint: String): Unit = {
    endpoints.get(endpoint) match {
      case Some(Endpoint(name, _, _)) =>
        endpoints -= name
        _removeEndpoint(name)
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown endpoint " + endpoint)
    }
  }

  /// These get implemented by the parent
  protected def _addPort(p: FEP.Port)
  protected def _removePort(port: String)
  protected def _addEndpoint(endpoint: String, port: String, config: List[Model.ConfigFile], publish: IProtocol.Publish, command: IProtocol.Respond): IProtocol.Issue
  protected def _removeEndpoint(endpoint: String)

}
