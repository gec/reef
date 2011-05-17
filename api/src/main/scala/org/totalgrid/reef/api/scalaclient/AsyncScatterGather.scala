package org.totalgrid.reef.api.scalaclient

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

import scala.collection.mutable.Map

object AsyncScatterGather {

  def collect[A <: AnyRef](promises: List[IPromise[Response[A]]])(callback: List[Response[A]] => Unit) {

    // the results we're collecting and a counter
    val map = Map.empty[Int, Response[A]]
    val size = promises.size

    def gather(idx: Int)(rsp: Response[A]) = map.synchronized {
      map += idx -> rsp
      if (map.size == size) callback(promises.indices.map(i => map(i)).toList) //last callback orders and calls the callback
    }

    promises.zipWithIndex.foreach {
      case (promise, i) => promise.listen(gather(i))
    }
  }

}

