/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.{ AnyNodeDestination, Destination }
import org.totalgrid.reef.sapi.client.{ Failure, Response, SessionPool }
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.protocol.api.Publisher
import org.totalgrid.reef.promise.{ SynchronizedPromise, Promise }

class IdentityOrderedPublisher[A](
  tx: OrderedServiceTransmitter,
  verb: Envelope.Verb,
  address: Destination,
  maxRetries: Int) extends OrderedPublisher[A, A](tx, verb, address, maxRetries)(x => x)

class OrderedPublisher[A, B](
    tx: OrderedServiceTransmitter,
    verb: Envelope.Verb,
    address: Destination,
    maxRetries: Int)(transform: A => B) extends Publisher[A] {

  def publish(value: A): Promise[Boolean] = tx.publish(transform(value), verb, address, maxRetries)
}

class OrderedServiceTransmitter(pool: SessionPool, maxQueueSize: Int = 100) extends Logging {

  private case class Record(value: Any, verb: Envelope.Verb, destination: Destination, maxRetries: Int, promise: SynchronizedPromise[Boolean])

  private val queue = new scala.collection.mutable.Queue[Record]
  private var transmitting = false

  def publish(value: Any,
    verb: Envelope.Verb = Envelope.Verb.POST,
    address: Destination = AnyNodeDestination,
    maxRetries: Int = 0): Promise[Boolean] = queue.synchronized {

    if (queue.size >= maxQueueSize) queue.wait

    val promise = new SynchronizedPromise[Boolean]
    queue.enqueue(Record(value, verb, address, maxRetries, promise))
    checkForTransmit()
    promise
  }

  private def checkForTransmit(): Boolean = {
    if (queue.size > 0) {
      transmitting = true
      val record = queue.dequeue()
      publish(record, record.maxRetries)
      true
    } else false
  }

  private def publish(record: Record, retries: Int): Unit = pool.borrow { s =>
    s.request(record.verb, record.value, destination = record.destination).listen(onResponse(record, retries))
  }

  private def onResponse(record: Record, retries: Int)(response: Response[Any]) = response match {
    case failure: Failure =>
      if (retries > 0) publish(record, retries - 1)
      else complete(record.promise, false)

    case _ => complete(record.promise, true)
  }

  private def complete(promise: SynchronizedPromise[Boolean], result: Boolean) = {
    promise.onResponse(result)
    queue.synchronized {
      transmitting = false
      if (!checkForTransmit()) queue.notify()
    }
  }

}