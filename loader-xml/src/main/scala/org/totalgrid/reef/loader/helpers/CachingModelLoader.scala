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
import org.totalgrid.reef.util.Logging
import collection.mutable.Map

class CachingModelLoader(client: Option[RestOperations], create: Boolean = true) extends ModelLoader with Logging {
  private var puts = List.empty[AnyRef]
  private val triggers = scala.collection.mutable.Map.empty[String, TriggerSet]
  private val modelContainer = new ModelContainer

  def putOrThrow(entity: Entity) {
    puts ::= entity;
    modelContainer.add(entity);
    autoFlush
  }

  def putOrThrow(edge: EntityEdge) {
    puts ::= edge;
    modelContainer.add(edge);
    autoFlush
  }

  def putOrThrow(entityAttributes: EntityAttributes) {
    puts ::= entityAttributes;
    modelContainer.add(entityAttributes);
    autoFlush
  }

  def putOrThrow(command: Command) {
    puts ::= command;
    modelContainer.add(command);
    autoFlush
  }

  def putOrThrow(point: Point) {
    puts ::= point;
    modelContainer.add(point);
    autoFlush
  }

  def putOrThrow(eventConfig: EventConfig) {
    puts ::= eventConfig;
    modelContainer.add(eventConfig);
    autoFlush
  }

  def putOrThrow(endpointConfig: CommEndpointConfig) {
    puts ::= endpointConfig;
    modelContainer.add(endpointConfig);
    autoFlush
  }

  def putOrThrow(configFile: ConfigFile) {
    puts ::= configFile;
    modelContainer.add(configFile);
    autoFlush
  }

  def putOrThrow(commChannel: CommChannel) {
    puts ::= commChannel;
    modelContainer.add(commChannel);
    autoFlush
  }

  def putOrThrow(triggerSet: TriggerSet) {
    triggers.put(triggerSet.getPoint.getName, triggerSet);
    modelContainer.add(triggerSet)
    autoFlush
  }

  def getOrThrow(e: TriggerSet): List[TriggerSet] =
    {
      client.map {
        _.get(e).await().expectMany()
      }.getOrElse(triggers.get(e.getPoint.getName).map {
        _ :: Nil
      }.getOrElse(Nil))
    }

  def autoFlush {
    client.foreach(flush(_, None))
  }

  def getTriggerSets(): Map[String, TriggerSet] = {
    triggers.clone()
  }

  def flush(client: RestOperations, progressMeter: Option[ResponseProgressRenderer]) =
    {
      logger.debug("flushing")
      progressMeter.foreach(_.start(puts.size + triggers.size))

      def handle[A <: AnyRef](promise: Promise[Response[A]], request: AnyRef) {
        val response = promise.await

        // check the response for any non-successful requests
        try {
          response.expectMany()
        } catch {
          case rse: ReefServiceException =>
            // attach a helpful error message with exact data that caused failure
            val errorMessage = "Error processing object of type: " + request.getClass.getSimpleName + ", with data: " + request
            throw new ReefServiceException(errorMessage, rse.getStatus, rse)
        }

        progressMeter.foreach(_.update(response.status, request))
      }

      val uploadOrder = (puts.reverse ::: triggers.map {
        _._2
      }.toList)

      if (create) {
        uploadOrder.foreach { x => handle(client.put(x), x) }
      } else {
        uploadOrder.reverse.foreach { x => handle(client.delete(x), x) }
      }

      reset()
      progressMeter.foreach(_.finish)
    }

  def getModelContainer: ModelContainer =
    {
      modelContainer
    }

  def reset() {
    puts = List.empty[AnyRef]
    modelContainer.reset
    triggers.clear
  }

  def size = puts.size + triggers.keys.size
}

