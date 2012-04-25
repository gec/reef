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

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException
import java.util.UUID

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

  // TODO: implement convertToProtos for all service types
  /**
   * convert all of the objects to protos. This is done as a list so we can effeciently load all of data in as few
   * roundtrips to the database as possible.
   */
  def convertToProtos(context: RequestContext, entries: List[ModelType]): List[MessageType] =
    entries.map { convertToProto(_) }

  final def convertAProto(context: RequestContext, entry: ModelType): MessageType = convertToProtos(context, List(entry)).head

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

  /**
   * before returning the results to the user we need to make sure they are sorted
   * into a stable and sensible order.
   */
  def sortResults(list: List[MessageType]): List[MessageType]

  /**
   * gets the entities associated with the resource so we can get any necessary
   * authorization checks.
   */
  def relatedEntities(entries: List[ModelType]): List[UUID]
}

/**
 * this trait is used for models that don't need to do anything complicated during model creation,
 * more complex models will implement createFromProto and updateFromProto directly
 */
trait SimpleModelEntryCreation[MessageType, ModelType] extends ServiceModel[MessageType, ModelType] {

  def createFromProto(context: RequestContext, req: MessageType): ModelType =
    create(context, createModelEntry(context, req))

  override def updateFromProto(context: RequestContext, proto: MessageType, existing: ModelType): (ModelType, Boolean) =
    update(context, updateModelEntry(context, proto, existing), existing)

  /**
   * Convert message type to model type when creating
   * @param proto   Message type
   * @return        Model type
   */
  def createModelEntry(context: RequestContext, proto: MessageType): ModelType

  /**
   * Create a modified model type using message type and existing model type
   * @param proto     Message type
   * @param existing  Existing model entry
   * @return          Updated model entry
   */
  def updateModelEntry(context: RequestContext, proto: MessageType, existing: ModelType): ModelType =
    createModelEntry(context, proto)
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

  protected def publishEvent(context: RequestContext, event: Envelope.SubscriptionEventType, resp: MessageType, key: String): Unit = {
    context.eventPublisher.publishEvent(event, resp, key)
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
    keys.foreach(context.eventPublisher.bindQueueByClass(queue, _, req.getClass))
  }
}

