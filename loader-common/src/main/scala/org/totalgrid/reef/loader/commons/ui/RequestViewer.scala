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
package org.totalgrid.reef.loader.commons.ui

import java.io.PrintStream
import org.totalgrid.reef.clientapi.sapi.client.{ Response, RequestSpy }
import net.agileautomata.executor4s.Future
import org.totalgrid.reef.clientapi.proto.Envelope.{ BatchServiceRequest, Status, Verb }

class RequestViewer(stream: PrintStream, total: Int, width: Int = 50) extends RequestSpy {

  def onRequestReply[A](verb: Verb, request: A, future: Future[Response[A]]) = {
    if (request.asInstanceOf[AnyRef].getClass != classOf[BatchServiceRequest]) {
      future.listen { response =>
        update(response.status, request.asInstanceOf[AnyRef])
      }
    }
  }

  start

  class Counter {
    var sum = 1
    def increment() = sum += 1
  }

  var counts = Map.empty[Status, Counter]
  var classCounts = Map.empty[Class[_], Counter]
  var handled: Int = 0
  val startTime = System.nanoTime()

  def start = {
    handled = 0

    stream.println("Processing " + total + " objects")

    stream.print("%6d of %6d ".format(handled, total))
    stream.print("|")
    stream.flush()
  }

  private def getStatusChar(status: Status): String = {
    status match {
      case Status.OK => "o"
      case Status.CREATED => "+"
      case Status.UPDATED => "*"
      case Status.NOT_MODIFIED => "."
      case Status.DELETED => "-"
      case _ => "!"
    }
  }

  private def update(status: Status, request: AnyRef) = {

    stream.print(getStatusChar(status))

    handled += 1
    if (handled % width == 0) {
      stream.print("\n%6d of %6d  ".format(handled, total))
    }

    //stream.print(status.toString + "-" + request.getClass.getSimpleName + "\n")
    stream.flush()

    counts.get(status) match {
      case Some(current) => current.increment
      case None => counts += status -> new Counter
    }
    classCounts.get(request.getClass) match {
      case Some(current) => current.increment
      case None => classCounts += request.getClass -> new Counter
    }
  }

  def finish = {
    stream.println("|")
    stream.println("Statistics. Finished in: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds")
    counts.foreach {
      case (status, count) =>
        stream.println("\t" + status.toString + "(" + getStatusChar(status) + ")" + " : " + count.sum)
    }
    stream.println("Types: ")
    classCounts.foreach { case (classN, count) => stream.println("\t" + classN.getSimpleName + " : " + count.sum) }
    stream.println
  }
}
