/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.reactor.{ Reactable, Lifecycle }
import org.totalgrid.reef.app.ServiceContext

import org.totalgrid.reef.event._
import org.totalgrid.reef.messaging._
import org.totalgrid.reef.protoapi.ProtoServiceTypes.{ SingleResponse, Failure }

import org.totalgrid.reef.protocol.api.{ IProtocol => Protocol }

import org.totalgrid.reef.proto.FEP.{ CommunicationEndpointConnection => ConnProto, FrontEndProcessor }
import org.totalgrid.reef.proto.Application.ApplicationConfig

import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Conversion.convertIterableToMapified
import org.totalgrid.reef.app.{ ServiceHandler }

object FrontEndActor {
  val retryms = 5000
}

abstract class FrontEndActor(registry: ProtoRegistry, protocols: Seq[Protocol], eventLog: EventLogPublisher, appConfig: ApplicationConfig, retryms: Long)
    extends Reactable with Lifecycle with ServiceHandler with ServiceContext[ConnProto] with FEPOperations with Logging {

  //helper objects that sets up all of the services/pulishers from abstract registries
  val services = new FrontEndServices(registry)
  val connections = new FrontEndConnections(protocols, registry, this)

  /* ---- Implement ServiceContext[Endpoint] ---- */

  // all of the objects we receive here are incomplete we need to request
  // the full object tree for them
  def add(ep: ConnProto) = {
    load(ep) { result =>
      execute {
        tryWrap("Error adding connProto: " + result) {
          // the coordinator assigns FEPs when available but meas procs may not be online yet
          // re sends with routing information when meas_proc is online
          if (result.hasRouting) connections.add(result)
          else connections.remove(result)
        }
      }
    }
  }

  def remove(ep: ConnProto) = {
    execute {
      tryWrap("Error removing connProto: " + ep) {
        connections.remove(ep)
      }
    }
  }

  def modify(ep: ConnProto) = {
    load(ep) { result =>
      execute {
        tryWrap("Error modifying connProto: " + result) {
          if (result.hasRouting) connections.modify(result)
          else connections.remove(result)
        }
      }
    }
  }

  // don't do anything
  def subscribed(list: List[ConnProto]) = {
    load(list) { result =>
      execute {
        tryWrap("Error adding list: " + result.size) {
          result.foreach { ep => connections.add(ep) }
        }
      }
    }
  }

  /* ---- Done implementing ServiceContext[Endpoint] ---- */

  override def afterStart() = annouce

  override def beforeStop() = {
    info { "Clearing Connections..." }
    connections.clear
  }

  // blocking function, uses a service to retrieve the fep uid
  private def annouce(): Unit = {

    eventLog.event(EventType.System.SubsystemStarting)

    val msg = protocols.foldLeft(FrontEndProcessor.newBuilder) { (msg, p) =>
      msg.addProtocols(p.name)
    }.setAppConfig(appConfig).build

    services.frontend.asyncPutOne(msg) {
      _ match {
        case SingleResponse(fem) =>
          eventLog.event(EventType.System.SubsystemStarted)
          info { "Got uid: " + fem.getUid }
          val query = ConnProto.newBuilder.setFrontEnd(fem).build
          // this is where we actually bind up the service calls
          this.addServiceContext(registry, retryms, ConnProto.parseFrom, query, this)
        case x: Failure =>
          warn(x)
          delay(retryms) { annouce }
      }
    }
  }

  /**
   * when setting up asynchronous callbacks it is doubly important to catch exceptions 
   * near where they are thrown or else they will bubble all the way up into the calling code
   */
  private def tryWrap[T](msg: String)(fun: => T) {
    try {
      fun
    } catch {
      case t: Throwable => error(msg + ": " + t)
    }
  }
}

