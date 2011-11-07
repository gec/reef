/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.api.protocol.simulator

import org.totalgrid.reef.proto.{ SimMapping, Model, Commands }

import org.totalgrid.reef.api.protocol.api._
import net.agileautomata.executor4s._
import org.totalgrid.reef.proto.SimMapping.SimulatorMapping
import org.totalgrid.reef.proto.FEP.CommChannel
import com.weiglewilczek.slf4s.Logging

/**
 * Protocol implementation that creates and manages simulators to test system behavior
 * under configurable load.
 */
class SimulatedProtocol(exe: Executor) extends ChannelIgnoringProtocol with SimulatorManagement with Logging {

  import Protocol._

  final override def name: String = "benchmark"
  final override def requiresChannel = false

  case class PluginRecord(endpoint: String, mapping: SimulatorMapping, publisher: BatchPublisher, current: Option[SimulatorPlugin])

  private val mutex = new Object
  private var endpoints = Map.empty[String, PluginRecord]
  private var factories = Set.empty[SimulatorPluginFactory]

  override def addEndpoint(
    endpoint: String,
    channel: String,
    files: List[Model.ConfigFile],
    batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher): CommandHandler = mutex.synchronized {

    endpoints.get(endpoint) match {
      case Some(x) =>
        throw new IllegalStateException("Endpoint has already been added: " + endpoint)
      case None =>
        val file = Protocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping").getFile
        val mapping = SimMapping.SimulatorMapping.parseFrom(file)
        val emptyRecord = new PluginRecord(endpoint, mapping, batchPublisher, None)
        endpoints += endpoint -> emptyRecord
        checkEndpoint(emptyRecord)
        new EndpointCommandHandler(endpoint)
    }
  }

  override def removeEndpoint(endpoint: String) = mutex.synchronized {
    endpoints.get(endpoint) match {
      case Some(record) =>
        record.current.foreach(_.shutdown()) // shutdown the current plugin
        endpoints -= endpoint
      case None =>
        throw new IllegalStateException("Trying to remove endpoint that doesn't exist: " + endpoint)
    }
  }

  class EndpointCommandHandler(endpoint: String) extends CommandHandler {

    def buildResponse(cmd: Commands.CommandRequest, status: Commands.CommandStatus) =
      Commands.CommandResponse.newBuilder.setCorrelationId(cmd.getCorrelationId).setStatus(status).build

    def issue(cmd: Commands.CommandRequest, publisher: Protocol.ResponsePublisher): Unit = mutex.synchronized {
      endpoints.get(endpoint) match {
        case Some(record) =>
          val status = record.current match {
            case Some(current) => current.issue(cmd)
            case None =>
              logger.error("Benchmark protocol received command for endpoint, but no plugin was loaded for endpoint: " + endpoint)
              Commands.CommandStatus.NOT_SUPPORTED
          }
          publisher.publish(buildResponse(cmd, status))
        case None =>
          logger.error("Benchmark protocol received command for unregistered endpoint: " + endpoint)
      }
    }
  }

  def checkEndpoints() = endpoints.foreach { case (endpoint, record) => checkEndpoint(record) }

  def checkEndpoint(record: PluginRecord) = {

    case class Result(endpoint: String, level: Int, factory: SimulatorPluginFactory, current: Option[SimulatorPlugin])

    def max(best: Option[Result], x: Result) = best match {
      case None => Some(x)
      case Some(current) => if (current.level >= x.level) best else Some(x)
    }

    def add(endpoint: String, executor: Executor, publisher: BatchPublisher, mapping: SimulatorMapping, factory: SimulatorPluginFactory) = {
      val simulator = factory.create(endpoint, Strand(executor), publisher, mapping)
      logger.info("Adding simulator for endpoint " + endpoint + " of type " + simulator.getClass.getName)
      endpoints += endpoint -> PluginRecord(endpoint, mapping, publisher, Some(simulator))
    }

    def checkResult(result: Result, record: PluginRecord) = result.current match {
      case Some(x) => if (!x.factory.equals(result.factory)) {
        logger.info("Replacing simulator for endpoint " + result.endpoint + " of type " + x.getClass.getName)
        x.shutdown()
        add(record.endpoint, exe, record.publisher, record.mapping, result.factory)
      }
      case None =>
        add(record.endpoint, exe, record.publisher, record.mapping, result.factory)
    }

    val results = factories.map(fac => Result(record.endpoint, fac.getSimLevel(record.endpoint, record.mapping), fac, record.current))
    val best = results.foldLeft(None: Option[Result])((best, x) => max(best, x))
    best.foreach(result => checkResult(result, record))
  }

  def addPluginFactory(factory: SimulatorPluginFactory): Unit = mutex.synchronized {
    factories += factory
    checkEndpoints()
  }

  def removePluginFactory(factory: SimulatorPluginFactory): Unit = mutex.synchronized {
    factories -= factory
    endpoints.foreach {
      case (endpoint, record) => record.current.foreach { plugin =>
        if (factory.equals(plugin.factory)) {
          plugin.shutdown()
          val emptyRecord = PluginRecord(endpoint, record.mapping, record.publisher, None)
          endpoints += endpoint -> emptyRecord
          checkEndpoint(emptyRecord)
        }
      }
    }
  }

  override def getSimulators() = {
    endpoints.map { case (n, r) => n -> r.current }.toMap
  }

}