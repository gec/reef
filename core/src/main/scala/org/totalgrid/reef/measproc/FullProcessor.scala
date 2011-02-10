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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.event.EventType._
import org.totalgrid.reef.frontend.KeyedMap

import org.totalgrid.reef.reactor.{ Reactable, Lifecycle, LifecycleManager }
import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto }

import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.app.{ ServiceHandler, CoreApplicationComponents, ServiceContext }
import org.totalgrid.reef.util.BuildEnv.ConnInfo
import org.totalgrid.reef.persistence.{ InMemoryObjectCache }
import org.totalgrid.reef.measurementstore.{ MeasurementStoreToMeasurementCacheAdapter, MeasurementStoreFinder }

abstract class ConnectionHandler(fun: ConnProto => MeasurementStreamProcessingNode)
    extends ServiceHandler with ServiceContext[ConnProto] with KeyedMap[ConnProto]
    with Reactable with Lifecycle {

  def getKey(c: ConnProto) = c.getUid

  var map = Map.empty[String, MeasurementStreamProcessingNode]

  def addEntry(ep: ConnProto) = {
    val entry = fun(ep)
    map += ep.getUid -> entry
    entry.start
  }

  def removeEntry(ep: ConnProto) = {
    map.get(ep.getUid).get.stop
    map -= ep.getUid
  }

  def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.getAssignedTime != existing.getAssignedTime
  }
}

/**  Non-entry point meas processor setup
 */
class FullProcessor(components: CoreApplicationComponents, measStoreConfig: ConnInfo) extends Logging with Lifecycle {

  var lifecycles = new LifecycleManager(List(components.heartbeatActor, components.nonopPublisher))

  // caches used to store measurements and overrides
  val measStore = MeasurementStoreFinder.getInstance(measStoreConfig, lifecycles.add _)
  val measCache = new MeasurementStoreToMeasurementCacheAdapter(measStore)

  // TODO: make override caches configurable like measurement store

  val overCache = new InMemoryObjectCache[Measurement]
  val triggerStateCache = new InMemoryObjectCache[Boolean]

  val connectionHandler = new ConnectionHandler(addStreamProcessor(_)) with ReactActor

  // TODO : verify order of startup/shutdown

  override def doStart() {
    components.logger.event(System.SubsystemStarting)
    lifecycles.start
    subscribeToStreams
    components.logger.event(System.SubsystemStarted)
  }

  override def doStop() {
    components.logger.event(System.SubsystemStopping)
    connectionHandler.clear
    lifecycles.stop
    components.logger.event(System.SubsystemStopped)
  }

  def addStreamProcessor(streamConfig: ConnProto): MeasurementStreamProcessingNode = {
    val reactor = new ReactActor {}
    val streamHandler = new MeasurementStreamProcessingNode(components.amqp, components.registry, measCache, overCache, triggerStateCache, streamConfig, reactor)
    streamHandler.setHookSource(components.metricsPublisher.getStore("measproc-" + streamConfig.getLogicalNode.getName))
    // TODO: figure out how to link the start/stop of this object
    streamHandler
  }

  def subscribeToStreams() = {
    val connection = ConnProto.newBuilder.setMeasProc(components.appConfig).build
    connectionHandler.addServiceContext(components.registry, 5000, ConnProto.parseFrom, connection, connectionHandler)
    connectionHandler.start
  }
}
