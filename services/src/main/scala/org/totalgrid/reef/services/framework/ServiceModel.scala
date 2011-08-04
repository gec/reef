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
package org.totalgrid.reef.services.framework

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.japi.{ BadRequestException, Envelope }

/**
 * Interface for generic use of models by simple REST services
 */
trait ServiceModel[MessageType, ModelType]
    extends ModelCrud[ModelType] {

  /**
   * Subscribe to model events
   *  @param req      Specifies events to be routed
   *  @param queue    Queue provided by the messaging system
   */
  def subscribe(context: RequestContext, req: MessageType, queue: String): Unit

  def createFromProto(context: RequestContext, req: MessageType): ModelType

  def updateFromProto(context: RequestContext, proto: MessageType, existing: ModelType): (ModelType, Boolean) =
    throw new BadRequestException("Cannot update this object")

  /**
   * Convert model entry to message type
   * @param entry   Model entry
   * @return        Message type
   */
  def convertToProto(entry: ModelType): MessageType

  /**
   * Find a unique model entry given a message request
   * @param req   Message type descriptor of model entries
   * @return      Optional model type, None if does not exist or more than one
   */
  def findRecord(context: RequestContext, req: MessageType): Option[ModelType]

  /**
   * Find zero or more model entries given a message request
   * @param req   Message type descriptor of model entries
   * @return      List of model entries that much request
   */
  def findRecords(context: RequestContext, req: MessageType): List[ModelType]
}

/**
 * this trait is used for models that don't need to do anything complicated during model creation,
 * more complex models will implement createFromProto and updateFromProto directly
 */
trait SimpleModelEntryCreation[MessageType, ModelType] extends ServiceModel[MessageType, ModelType] {

  def createFromProto(context: RequestContext, req: MessageType): ModelType =
    create(context, createModelEntry(req))

  override def updateFromProto(context: RequestContext, proto: MessageType, existing: ModelType): (ModelType, Boolean) =
    update(context, updateModelEntry(proto, existing), existing)

  /**
   * Convert message type to model type when creating
   * @param proto   Message type
   * @return        Model type
   */
  def createModelEntry(proto: MessageType): ModelType

  /**
   * Create a modified model type using message type and existing model type
   * @param proto     Message type
   * @param existing  Existing model entry
   * @return          Updated model entry
   */
  def updateModelEntry(proto: MessageType, existing: ModelType): ModelType =
    createModelEntry(proto)
}

/**
 * Composed trait for the implementation of the service bridge of
 * models (event buffering/publishing, subscribe requests)
 */
trait EventedServiceModel[MessageType <: GeneratedMessage, ModelType]
    extends ServiceEventBuffering[MessageType, ModelType]
    with ServiceEventPublishing[MessageType]
    with ServiceEventSubscribing[MessageType] {
}

/**
 * Composed trait of model observing/event buffering component
 */
trait ServiceEventBuffering[MessageType <: GeneratedMessage, ModelType]
    extends EventQueueingObserver[MessageType, ModelType]
    with SubscribeEventCreation[MessageType, ModelType] {
}

/**
 * Implementation of publishing events using a subscription handler
 */
trait ServiceEventPublishing[MessageType <: GeneratedMessage] {

  protected def publishEvent(context: RequestContext, event: Envelope.Event, resp: MessageType, key: String): Unit = {
    context.subHandler.publish(event, resp, key)
  }
}

/**
 * Implementation of passing subscribe requests to the subscription handler with routing information
 */
trait ServiceEventSubscribing[MessageType <: GeneratedMessage] extends SubscribeFunctions[MessageType] {

  /**
   * Subscribe to model events
   *  @param req      Specifies events to be routed
   *  @param queue    Queue provided by the messaging system
   */
  def subscribe(context: RequestContext, req: MessageType, queue: String): Unit = {
    val keys = getSubscribeKeys(req)
    keys.foreach(context.subHandler.bind(queue, _, req))
  }
}

