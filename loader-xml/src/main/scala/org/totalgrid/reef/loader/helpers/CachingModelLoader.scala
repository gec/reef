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

import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.Alarms._
import org.totalgrid.reef.client.service.proto.FEP._
import org.totalgrid.reef.client.service.proto.Processing._
import org.totalgrid.reef.loader.ModelLoader
import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.loader.commons.LoaderServices
import org.totalgrid.reef.loader.commons.ui.RequestViewer

import java.io.PrintStream
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.service.proto.Auth.{ Agent, PermissionSet }
import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.operations.scl.ScalaRequestListener._
import org.totalgrid.reef.client.operations.scl.ScalaBatchOperations._

// TODO: get rid of caching model loader
class CachingModelLoader(client: Option[LoaderServices], batchSize: Int = 25) extends ModelLoader with Logging {
  private var puts = List.empty[AnyRef]
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

  def putOrThrow(entityAttribute: EntityAttribute) {
    puts ::= entityAttribute;
    modelContainer.add(entityAttribute);
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

  def putOrThrow(endpointConfig: Endpoint) {
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
    puts ::= triggerSet;
    modelContainer.add(triggerSet)
    autoFlush
  }

  def putOrThrow(e: Calculation) = {
    puts ::= e
    modelContainer.add(e)
    autoFlush
  }

  def putOrThrow(e: PermissionSet) = {
    puts ::= e
    autoFlush
  }

  def putOrThrow(e: Agent) = {
    puts ::= e
    autoFlush
  }

  def autoFlush {
    client.foreach(flush(_, None))
  }

  def flush(client: LoaderServices, stream: Option[PrintStream]) = {

    val uploadOrder = puts.reverse.toList
    val uploadActions: List[LoaderServices => Promise[_]] = uploadOrder.map { eq => (c: LoaderServices) => c.put(eq) }

    val viewer = stream.map { new RequestViewer(_, uploadActions.size) }

    try {
      withRequestListener(client, viewer) {
        batchOperations(client, batchSize) {
          uploadOrder.foreach { client.put(_) }
        }.await
      }
    } finally {
      viewer.foreach { _.finish }
    }

    reset()
  }

  def getModelContainer: ModelContainer = {
    modelContainer
  }

  def reset() {
    puts = List.empty[AnyRef]
    modelContainer.reset
  }

  def size = puts.size
  def allProtos = puts.reverse
}

