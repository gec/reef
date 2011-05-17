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
package org.totalgrid.reef.loader

import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.api.scalaclient.SyncOperations

trait ModelLoader {
  def putOrThrow(e: Entity)
  def putOrThrow(e: EntityEdge)
  def putOrThrow(e: Command)
  def putOrThrow(e: Point)
  def putOrThrow(e: EventConfig)
  def putOrThrow(e: CommEndpointConfig)
  def putOrThrow(e: ConfigFile)
  def putOrThrow(e: CommChannel)

  def putOrThrow(e: TriggerSet)
  def getOrThrow(e: TriggerSet): List[TriggerSet]
}

class CachingModelLoader(client: Option[SyncOperations]) extends ModelLoader {

  private var puts = List.empty[GeneratedMessage]

  def putOrThrow(e: Entity) = { puts ::= e; autoFlush }
  def putOrThrow(e: EntityEdge) = { puts ::= e; autoFlush }
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
    client.foreach(flush(_))
  }

  def flush(client: SyncOperations) = {
    puts.reverse.foreach { x => client.put(x).await().expectMany() }
    triggers.foreach { case (name, tset) => client.put(tset).await().expectMany() }
    puts = Nil
    triggers.clear
  }

  def size = puts.size + triggers.keys.size
}