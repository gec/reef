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

package org.totalgrid.reef.messaging

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.client.{ Failure, Response, SessionPool }
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.promise.{ FixedPromise, SynchronizedPromise, Promise }
import org.totalgrid.reef.sapi.{ BasicRequestHeaders, AnyNodeDestination, Routable }

/**
 * allows for the ordered publishing of service requests to multiple services to be completed in the order
 * they were inserted into the publishing buffer. Producers are throttled when the more than maxQueueSize
 * messages are in the buffer already. When the ordered transmitter is no longer needed shutdown should be
 * called to flush all of the pending messages and stop any more being queued.
 */
class OrderedServiceTransmitter(pool: SessionPool, maxQueueSize: Int = 100) extends Logging {
  private case class Record(value: Any, verb: Envelope.Verb, destination: Routable, maxRetries: Int, promise: SynchronizedPromise[Boolean])

  private val queue = new scala.collection.mutable.Queue[Record]
  private var transmitting = false
  private var isShutdown = false

  def publish(value: Any, verb: Envelope.Verb = Envelope.Verb.POST, address: Routable = AnyNodeDestination,
    maxRetries: Int = 0): Promise[Boolean] = queue.synchronized {
    if (isShutdown) {
      // could throw exception instead if all producers are guaranteed to be shutdown before
      // the transmitter is stopped
      new FixedPromise[Boolean](false)
    } else {
      while (queue.size >= maxQueueSize) {
        logger.info("Publisher waiting. " + this)
        queue.wait
      }

      val promise = new SynchronizedPromise[Boolean]
      queue.enqueue(Record(value, verb, address, maxRetries, promise))
      checkForTransmit()
      promise
    }
  }

  private def checkForTransmit(): Boolean =
    {
      if (!transmitting && !queue.isEmpty) {
        transmitting = true
        val record = queue.dequeue()
        publish(record, record.maxRetries)
        true
      } else {
        false
      }
    }

  private def publish(record: Record, retries: Int): Unit = pool.borrow { s =>
    val headers = BasicRequestHeaders.empty.setDestination(record.destination)
    s.request(record.verb, record.value, headers).listen(onResponse(record, retries))
  }

  private def onResponse(record: Record, retries: Int)(response: Response[Any]) = response match {
    case failure: Failure =>
      logger.warn("ordered publisher failure: " + failure)
      if (retries > 0) {
        publish(record, retries - 1)
      } else {
        complete(record.promise, false)
      }
    case _ => complete(record.promise, true)
  }

  private def complete(promise: SynchronizedPromise[Boolean], result: Boolean) =
    {
      promise.onResponse(result)
      queue.synchronized {
        transmitting = false
        if (!checkForTransmit()) {
          queue.notifyAll()
        }
      }
    }

  /**
   * waits until all messages have been sent successfully or failed out
   */
  def flush() = queue.synchronized {
    assert(transmitting || queue.isEmpty)
    while (transmitting || !queue.isEmpty) {
      queue.wait()
    }
  }

  /**
   * stop new messages from being queued and wait for flush to complete
   */
  def shutdown() = queue.synchronized {
    isShutdown = true
    flush()
  }

  override def toString =
    {
      "OrderedServiceTransmitter: isShutdown: " + isShutdown + ", transmitting: " + transmitting + ", queue: " + queue.size + ", maxQueueSize: " + maxQueueSize
    }
}

