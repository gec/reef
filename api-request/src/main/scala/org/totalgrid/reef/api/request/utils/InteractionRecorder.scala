package org.totalgrid.reef.api.request.utils

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
import xml.Node
import scala.collection.mutable.Queue

import org.totalgrid.reef.api.{ RequestEnv, Envelope, IDestination, AnyNode }
import org.totalgrid.reef.api.scalaclient.MultiResult
import org.totalgrid.reef.api.scalaclient.{ DefaultHeaders, SyncOperations }

/**
 * the interaction recorder is a wrapper we can add around a SyncOperations client to collect a set of
 * request/response interactions and human readable prose describing what we are doing. Since we are instrumenting
 * the lowest level operation in this class (request) we get to see the result before any exception throwing would
 * occur therefore the output is just as useful in the failure cases (expected and unexpected)
 */
trait InteractionRecorder extends SyncOperations { self: DefaultHeaders =>

  // list of explanations for the upcoming requests
  private val explanations = new Queue[Documenter.CaseExplanation]
  // explanations joined with the actual request/response and verb to complete the description
  private var explainedRequests: List[Documenter.RequestWithExplanation[_ <: AnyRef]] = Nil

  /**
   * add an explanation for the next request we are going to do with the client. Multiple operations
   * can be queued up and applied in order to the requests
   */
  def addExplanation(title: String, desc: Node): Unit = {
    explanations.enqueue(Documenter.CaseExplanation(title, desc))
  }

  /**
   * add a string based explanation (just put it inside a div)
   */
  def addExplanation(title: String, desc: String): Unit = addExplanation(title, <div>{ desc }</div>)

  /**
   * save out the recorded interaction as an xml file suitable for XSLT translation
   */
  def save(fileName: String, title: String, desc: Node = <div/>) = {
    val formattedRequests = explainedRequests.reverse.map { Documenter.getExplainedCase(_) }
    Documenter.save(fileName, formattedRequests, title, desc)
  }

  /**
   * implementation of SyncOperations base function that uses the passed in "real" client to create collect interactions
   * for later formatting to file
   */
  abstract override def request[A <: AnyRef](verb: Envelope.Verb, request: A, env: RequestEnv = getDefaultHeaders, destination: IDestination = AnyNode): MultiResult[A] = {

    val results = super.request(verb, request, env, destination)

    if (explanations.nonEmpty) {
      explainedRequests ::= Documenter.RequestWithExplanation(explanations.dequeue, verb, request, results)
    }

    results
  }
}