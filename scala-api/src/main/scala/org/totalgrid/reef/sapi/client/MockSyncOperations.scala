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
package org.totalgrid.reef.sapi.client

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

import scala.collection.mutable.Queue
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.{ RequestEnv, Destination, AnyNodeDestination }

/* TODO - rename this class, it's really a "queueing" SyncOperations */

/**
 * Mock the ISyncClientSession to collect all puts, posts, and deletes. A 'get' function
 * is specified upon construction.
 *
 * @param doGet    Function that is called for client.get
 * @param putQueue Mutable queue where puts & posts are enqueued.
 * @param delQueue Mutable queue where deletes are enqueued.
 *
 */
class MockSyncOperations(
    doGet: (AnyRef) => Response[AnyRef],
    putQueue: Queue[AnyRef] = Queue[AnyRef](),
    delQueue: Queue[AnyRef] = Queue[AnyRef]()) extends RestOperations with DefaultHeaders {

  /**
   * Reset all queues.
   */
  def reset = {
    putQueue.clear
    delQueue.clear
  }

  /**
   * Return the internal mutable queue where puts & posts are enqueued. Caller should use dequeue to get protos.
   */
  def getPutQueue = putQueue

  /**
   * Return the internal mutable queue where deletes are enqueued. Caller should use dequeue to get protos.
   */
  def getDelQueue = delQueue

  /**
   *   Assert that the elements in the internal putQueue are the same as
   * the elements in the specified putQ.
   * TODO: report which element is not equal (report the index, proto class diff, etc.).
   */
  def assertPuts(putQ: Queue[AnyRef]): Boolean = {
    putQueue == putQ
    //putQueue.corresponds( putQ)(_==_)
  }

  /**
   * Override request to define all of the verb helpers
   */
  final override def request[A](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, dest: Destination = AnyNodeDestination): Promise[Response[A]] = verb match {
    case Envelope.Verb.GET => new BasicPromise(doGet(payload.asInstanceOf[AnyRef]).asInstanceOf[Response[A]])
    case Envelope.Verb.DELETE =>
      delQueue.enqueue(payload.asInstanceOf[AnyRef])
      new BasicPromise(Success(Envelope.Status.OK, List[A](payload)))
    case Envelope.Verb.PUT =>
      putQueue.enqueue(payload.asInstanceOf[AnyRef])
      new BasicPromise(Success(Envelope.Status.OK, List[A](payload)))
    case Envelope.Verb.POST => throw new Exception("unimplemented")
  }

}

