/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.protocol.api.IListener
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.api.scalaclient.{ Failure, Success, Response, ISessionPool }
import org.totalgrid.reef.api.{ AnyNode, IDestination, Envelope, ReefServiceException }

import scala.annotation.tailrec

/**
 * Uses asynchronous methods and a queue to publish, blocking only when queue fills up
 */
/*
class AsyncQueuePublisher[A](pool: ISessionPool, verb: Envelope.Verb, destination: IDestination = AnyNode, maxSize : Int = 100)
  extends IListener[A] with Logging {

  assert(maxSize > 0)
  private val queue = new scala.collection.mutable.Queue[A]
  private var sending = false

  final override def onUpdate(value: A) = queue.synchronized {

    if(sending) {
      if(queue.size == maxSize) queue.wait()
      queue.enqueue(value)
    } else {
      send(value, retries)
    }

  }

  private def send(value: A) = pool.borrow { session =>
      try {
        session.request(verb, value, dest = dest).listen(onResponse(value, numRetries-1))
        sending = true
      }
      catch {
        case ex: ReefServiceException =>
          error(ex)
          stopSending()
      }
  }

  private def stopSending() = {
    sending = false
    queue.notifyAll()
  }

  private def pop()

  private def onResponse(value: A)(rsp: Response[A]) = queue.synchronized {

    sending = false

    rsp match {
      case x : Success =>
        if(queue.length > 0) {
          send(queue.dequeue, retries)
          queue.notifyAll()
        }
      case x : Failure =>
        error(msg)
        if(numRetries > 0) send(value, numRetries)
        else
    }

  }
}
*/ 