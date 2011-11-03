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
package org.totalgrid.reef.clientapi.sapi.client

object ScatterGather {

  /**
   * Asynchronous collect function calls back when all of the promises are complete
   */
  def collect[A](promises: List[Promise[A]])(callback: List[A] => Unit) {

    // the results we're collecting and a counter
    val map = scala.collection.mutable.Map.empty[Int, A]
    val size = promises.size

    if (size == 0) callback(Nil)
    else {
      // thread safe function that checks each response
      def gather(idx: Int)(rsp: Promise[A]) = map.synchronized {
        map += idx -> rsp.await
        if (map.size == size) callback(promises.indices.map(i => map(i)).toList) //last callback orders and calls the callback
      }

      // start listening on the promises
      promises.zipWithIndex.foreach {
        case (promise, i) => promise.listen(gather(i))
      }
    }
  }

}

