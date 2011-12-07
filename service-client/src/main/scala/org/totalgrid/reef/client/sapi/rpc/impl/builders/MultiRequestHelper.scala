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
package org.totalgrid.reef.client.sapi.rpc.impl.builders

import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.InstantExecutor

// TODO: migrate MultiRequestHelper functionality into executor4s
object MultiRequestHelper {
  def gatherResults[A](results: List[Result[A]]): Result[List[A]] = {
    val output = results.foldLeft(Success(List.empty[A]): Result[List[A]]) { (res: Result[List[A]], entry: Result[A]) =>
      res match {
        case Success(successfulResponse) =>
          entry match {
            case Success(l) => Success(l :: successfulResponse)
            case f2: Failure => f2
          }
        case f: Failure => f
      }
    }
    output.map { _.reverse }
  }

  def scatterGatherQuery[A, B](requests: Future[Result[List[A]]], fun: A => Future[Result[B]]): Future[Result[List[B]]] = {
    requests.flatMap { r =>
      r match {
        case Success(starts) =>
          val requests: List[Future[Result[B]]] = starts.map { entry => fun(entry) }

          val result: Future[Result[List[B]]] = Futures.gather(new InstantExecutor, requests).map { l => gatherResults(l.toList) }
          result

        case Failure(ex) => requests.replicate[Result[List[B]]]
      }
    }
  }
}