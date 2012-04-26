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
package org.totalgrid.reef.benchmarks.system

import org.totalgrid.reef.client.Client
import org.totalgrid.reef.loader.commons.LoaderServices
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point }
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointOwnership }
import scala.collection.JavaConversions._
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.sapi.client.Promise
import org.totalgrid.reef.util.Timing.Stopwatch

object ModelCreationUtilities {

  def getPointNames(endpointName: String, pointsPerEndpoint: Int) = {
    (1 to pointsPerEndpoint).map { i => endpointName + ".TestPoint" + i }
  }

  def addEndpoint(client: Client, endpointName: String, pointsPerEndpoint: Int, batchSize: Int) = {
    val loaderServices = client.getService(classOf[LoaderServices])
    loaderServices.startBatchRequests()

    val names = getPointNames(endpointName, pointsPerEndpoint)

    names.map { n => loaderServices.addPoint(Point.newBuilder.setName(n).setType(PointType.ANALOG).setUnit("raw").build) }

    val owner = EndpointOwnership.newBuilder.addAllPoints(names)

    val putEndpoint = Endpoint.newBuilder.setName(endpointName).setProtocol("null").setOwnerships(owner).build
    loaderServices.addEndpoint(putEndpoint)
    () => loaderServices.batchedFlushBatchRequests(batchSize)
  }

  def deleteEndpoint(client: Client, endpointName: String, pointsPerEndpoint: Int, batchSize: Int) = {
    val loaderServices = client.getService(classOf[LoaderServices])
    loaderServices.startBatchRequests()

    val names = getPointNames(endpointName, pointsPerEndpoint)

    loaderServices.delete(Endpoint.newBuilder.setName(endpointName).build)
    names.map { n => loaderServices.delete(Point.newBuilder.setName(n).build) }
    () => loaderServices.batchedFlushBatchRequests(batchSize)
  }

  /**
   * takes a list of operations and runs up to configurable number of them at the same time and collects
   * timing information on how long each individual request takes. The batchSize parameter is used to change
   * how much work is done in each request to the server.
   */
  def parallelExecutor[A](client: Client, numConcurrent: Int, batchableOperations: Seq[() => Promise[A]]) = {
    var inProgressOps = 0
    var remainingOps = batchableOperations
    var timingResults = List.empty[(Long, A)]

    val exe = client.getInternal.getExecutor

    val f = exe.future[Result[List[(Long, A)]]]
    val prom = Promise.from(f)

    def completed(stopwatch: Stopwatch, a: Promise[A]): Unit = f.synchronized {
      val result = a.extract
      if (result.isSuccess) {
        inProgressOps -= 1
        timingResults ::= (stopwatch.elapsed, result.get)
        if (timingResults.size == batchableOperations.size) f.set(Success(timingResults))
        else startNext()
      } else {
        f.set(Failure(result.toString))
      }
    }

    def startNext(): Unit = f.synchronized {
      if (inProgressOps < numConcurrent) {
        inProgressOps += 1
        val stopwatch = new Stopwatch()
        val nextOperationToStart = remainingOps.headOption
        if (nextOperationToStart.isDefined) {
          remainingOps = remainingOps.tail
          nextOperationToStart.get().listen(completed(stopwatch, _))
          startNext()
        }
      }
    }

    // kick off the executions
    startNext()

    // wait for them all to succeed or an exception to occur
    prom.await
  }
}