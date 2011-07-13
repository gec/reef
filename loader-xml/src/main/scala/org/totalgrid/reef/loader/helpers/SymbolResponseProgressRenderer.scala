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
package org.totalgrid.reef.loader.helpers

import org.totalgrid.reef.japi.Envelope.Status
import java.io.PrintStream

class SymbolResponseProgressRenderer(stream: PrintStream, width: Int = 50) extends ResponseProgressRenderer {

  class Counter {
    var sum = 1
    def increment() = sum += 1
  }

  var counts = Map.empty[Status, Counter]
  var handled: Int = 0
  var total: Int = 0

  def start(size: Int) = {
    total = size
    handled = 0

    stream.println("Uploading " + total + " objects to server")

    stream.print("%6d of %6d ".format(handled, total))
    stream.print("|")
    stream.flush()
  }

  def update(status: Status, request: AnyRef) = {
    val char = status match {
      case Status.OK => "o"
      case Status.CREATED => "+"
      case Status.UPDATED => "*"
      case Status.NOT_MODIFIED => "."
      case Status.DELETED => "-"
      case _ => "!"
    }
    stream.print(char)

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
  }

  def finish = {
    stream.println("|")
    stream.println("Statistics: ")
    counts.foreach { case (status, count) => stream.println(status.toString + " : " + count.sum) }
    stream.println
  }
}

