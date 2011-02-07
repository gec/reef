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
package org.totalgrid.reef.protoapi.client

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes._
import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage
import scala.collection.mutable.Queue

/**
 * Mock the SyncServiceClient to collect all puts, posts, and deletes. A 'get' function
 * is specified upon construction.
 *
 * @param doGet    Function that is called for client.get
 * @param putQueue Mutable queue where puts & posts are enqueued.
 * @param delQueue Mutable queue where deletes are enqueued.
 *
 */
class MockSyncOperations(
    doGet: (GeneratedMessage) => MultiResult[GeneratedMessage],
    putQueue: Queue[GeneratedMessage] = Queue[GeneratedMessage](),
    delQueue: Queue[GeneratedMessage] = Queue[GeneratedMessage]()) extends SyncOperations {

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
  def assertPuts(putQ: Queue[GeneratedMessage]): Boolean = {
    putQueue == putQ
    //putQueue.corresponds( putQ)(_==_)
  }

  /**
   * This is not called.
   */
  override def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = MultiSuccess(List[A]())

  override def get[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = {
    doGet(payload).asInstanceOf[MultiResult[A]]
  }

  override def delete[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = {

    delQueue.enqueue(payload)
    MultiSuccess(List[A](payload))
  }

  override def post[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = {

    putQueue.enqueue(payload)
    MultiSuccess(List[A](payload))
  }

  override def put[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = {

    putQueue.enqueue(payload)
    MultiSuccess(List[A](payload))
  }

}

