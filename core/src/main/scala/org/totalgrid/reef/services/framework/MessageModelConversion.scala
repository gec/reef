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
package org.totalgrid.reef.services.framework

/**
 * Trait for coordinating between service message types and data model types
 */
trait MessageModelConversion[MessageType, T] {
  /**
   * Convert message type to model type when creating
   * @param proto   Message type
   * @return        Model type
   */
  def createModelEntry(proto: MessageType): T

  /**
   * Create a modified model type using message type and existing model type
   * @param proto     Message type
   * @param existing  Existing model entry
   * @return          Updated model entry
   */
  def updateModelEntry(proto: MessageType, existing: T): T = createModelEntry(proto)

  /**
   * Convert model entry to message type
   * @param entry   Model entry
   * @return        Message type
   */
  def convertToProto(entry: T): MessageType

  /**
   * Interpret message type as AMQP routing keys
   * @param req   Message type
   * @return      Routing key string
   */
  def getRoutingKey(req: MessageType): String

  /**
   * Find a unique model entry given a message request
   * @param req   Message type descriptor of model entries
   * @return      Optional model type, None if does not exist or more than one
   */
  def findRecord(req: MessageType): Option[T]

  /**
   * Find zero or more model entries given a message request
   * @param req   Message type descriptor of model entries
   * @return      List of model entries that much request
   */
  def findRecords(req: MessageType): List[T]
}
