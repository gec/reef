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
package org.totalgrid.reef.client.operations.scl

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.Promise
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.operations.impl.FuturePromise
import org.totalgrid.reef.client.exception.ReefServiceException

import java.util.{ List => JList }
import scala.collection.JavaConversions._

object PromiseCollators extends Logging {

  import ScalaPromise._

  // Gathers, using the first error as its failure case
  def collate[A](exe: Executor, promises: JList[Promise[A]]): Promise[JList[A]] = {
    collate(exe, promises.toList).map(x => x: JList[A])
  }

  // Gathers, using the first error as its failure case
  def collate[A](exe: Executor, promises: List[Promise[A]]): Promise[List[A]] = {

    val promise = FuturePromise.open[List[A]](exe)
    val size = promises.size
    val map = collection.mutable.Map.empty[Int, Promise[A]]

    def gather(i: Int)(prom: Promise[A]) {
      map.synchronized {
        map.put(i, prom)
        if (map.size == size) {
          val all = promises.indices.map(map(_))
          try {
            promise.setSuccess(all.map(_.await()).toList)
          } catch {
            case ex: ReefServiceException => promise.setFailure(ex)
            case x: Exception =>
              logger.error("Unhandled exception: " + x, x)
          }
        }
      }
    }

    if (promises.isEmpty) promise.setSuccess(Nil)
    else promises.zipWithIndex.foreach { case (prom, i) => prom.listenFor(gather(i)) }
    promise
  }
}
