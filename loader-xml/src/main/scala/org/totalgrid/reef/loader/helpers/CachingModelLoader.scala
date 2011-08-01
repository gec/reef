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

import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.sapi.client.{ Response, RestOperations }
import org.totalgrid.reef.loader.ModelLoader
import org.totalgrid.reef.japi.ReefServiceException

class CachingModelLoader(client: Option[RestOperations], create: Boolean = true) extends ModelLoader {

  private var puts = List.empty[AnyRef]

  def putOrThrow(e: Entity) = { puts ::= e; autoFlush }
  def putOrThrow(e: EntityEdge) = { puts ::= e; autoFlush }
  def putOrThrow(e: EntityAttributes) = { puts ::= e; autoFlush }
  def putOrThrow(e: Command) = { puts ::= e; autoFlush }
  def putOrThrow(e: Point) = { puts ::= e; autoFlush }
  def putOrThrow(e: EventConfig) = { puts ::= e; autoFlush }
  def putOrThrow(e: CommEndpointConfig) = { puts ::= e; autoFlush }
  def putOrThrow(e: ConfigFile) = { puts ::= e; autoFlush }
  def putOrThrow(e: CommChannel) = { puts ::= e; autoFlush }

  private val triggers = scala.collection.mutable.Map.empty[String, TriggerSet]

  def putOrThrow(e: TriggerSet) = { triggers.put(e.getPoint.getName, e); autoFlush }
  def getOrThrow(e: TriggerSet): List[TriggerSet] = {
    client.map {
      _.get(e).await().expectMany()
    }.getOrElse(
      triggers.get(e.getPoint.getName).map { _ :: Nil }.getOrElse(Nil))
  }

  def autoFlush = {
    client.foreach(flush(_, None))
  }

  def flush(client: RestOperations, progressMeter: Option[ResponseProgressRenderer]) = {
    progressMeter.foreach(_.start(puts.size + triggers.size))

    def handle[A <: AnyRef](promise: Promise[Response[A]], request: AnyRef) {
      val rsp = promise.await

      // check the response for any non-successful requests
      try {
        rsp.expectMany()
      } catch {
        case rse: ReefServiceException =>
          // attach a helpful error message with exact data that caused failure
          val errorMessage = "Error processing object: " + request.getClass.getSimpleName + " with data: " + request
          throw new ReefServiceException(errorMessage, rse.getStatus, rse)
      }

      progressMeter.foreach(_.update(rsp.status, request))
    }

    val uploadOrder = (puts.reverse ::: triggers.map { _._2 }.toList)

    if (create) uploadOrder.foreach { x => handle(client.put(x), x) }
    else uploadOrder.reverse.foreach { x => handle(client.delete(x), x) }

    puts = Nil
    triggers.clear

    progressMeter.foreach(_.finish)
  }

  def size = puts.size + triggers.keys.size
}

