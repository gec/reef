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

import org.totalgrid.reef.loader.commons.LoaderServices
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point }
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointOwnership }
import scala.collection.JavaConversions._
import net.agileautomata.executor4s._
import org.totalgrid.reef.util.Timing.Stopwatch
import org.totalgrid.reef.client.{ Promise, Client }
import org.totalgrid.reef.client.operations.impl.{ OpenPromise, FuturePromise }
import org.totalgrid.reef.client.exception.{ UnknownServiceException, ReefServiceException }
import org.totalgrid.reef.client.operations.scl.ScalaPromise._

object ModelCreationUtilities {

  def getPointNames(endpointName: String, pointsPerEndpoint: Int) = {
    (1 to pointsPerEndpoint).map { i => endpointName + ".TestPoint" + i }
  }

  def addEndpoint(client: Client, endpointName: String, pointsPerEndpoint: Int, batchSize: Int) = {
    val loaderServices = client.getService(classOf[LoaderServices])
    loaderServices.batching.start()

    val names = getPointNames(endpointName, pointsPerEndpoint)

    names.map { n => loaderServices.addPoint(Point.newBuilder.setName(n).setType(PointType.ANALOG).setUnit("raw").build) }

    val owner = EndpointOwnership.newBuilder.addAllPoints(names)

    val putEndpoint = Endpoint.newBuilder.setName(endpointName).setProtocol("null").setOwnerships(owner).build
    loaderServices.addEndpoint(putEndpoint)
    () => loaderServices.batching.flush(batchSize)
  }

  def deleteEndpoint(client: Client, endpointName: String, pointsPerEndpoint: Int, batchSize: Int) = {
    val loaderServices = client.getService(classOf[LoaderServices])

    val uuid = loaderServices.getEndpointByName(endpointName).await.getUuid
    loaderServices.disableEndpointConnection(uuid).await
    loaderServices.batching.start()

    val names = getPointNames(endpointName, pointsPerEndpoint)

    loaderServices.delete(Endpoint.newBuilder.setName(endpointName).build)
    names.map { n => loaderServices.delete(Point.newBuilder.setName(n).build) }
    () => loaderServices.batching.flush(batchSize)
  }

  /**
   * takes a list of operations and runs up to configurable number of them at the same time and collects
   * timing information on how long each individual request takes. The batchSize parameter is used to change
   * how much work is done in each request to the server.
   */
  def parallelExecutor[A](exe: Executor, numConcurrent: Int, batchableOperations: Seq[() => Promise[A]]) = {
    var inProgressOps = 0
    var remainingOps = batchableOperations
    var timingResults = List.empty[(Long, A)]

    val f = exe.future[Either[ReefServiceException, List[(Long, A)]]]
    val prom: OpenPromise[List[(Long, A)]] = FuturePromise.open(f)

    def completed(stopwatch: Stopwatch, a: Promise[A]) {
      f.synchronized {
        if (!prom.isComplete) {
          try {
            val result = a.await()
            inProgressOps -= 1
            timingResults ::= (stopwatch.elapsed, result)
            if (timingResults.size == batchableOperations.size) prom.setSuccess(timingResults)
            else startNext()
          } catch {
            case rse: ReefServiceException => prom.setFailure(rse)
            case ex: Throwable => prom.setFailure(new UnknownServiceException(ex.toString))
          }
        }
      }
    }

    def startNext() {
      f.synchronized {
        if (inProgressOps < numConcurrent) {
          inProgressOps += 1
          val stopwatch = Stopwatch.start
          val nextOperationToStart = remainingOps.headOption
          if (nextOperationToStart.isDefined) {
            remainingOps = remainingOps.tail
            nextOperationToStart.get().listenFor(completed(stopwatch, _))
            startNext()
          }
        }
      }
    }

    // kick off the executions
    startNext()

    // wait for them all to succeed or an exception to occur
    prom.await
  }
}