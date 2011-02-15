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

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.messaging.serviceprovider.ServiceSubscriptionHandler
import org.totalgrid.reef.api.{ Envelope, RequestEnv }

/**
 * Interface for generic use of models by simple REST services
 */
trait ServiceModel[MessageType, ModelType]
    extends ModelCrud[ModelType]
    //with MessageModelConversion[MessageType, ModelType]
    with EnvHolder {

  /**
   * Subscribe to model events
   *  @param req      Specifies events to be routed
   *  @param queue    Queue provided by the messaging system
   */
  def subscribe(req: MessageType, queue: String): Unit

  def createFromProto(req: MessageType): ModelType = {
    create(createModelEntry(req))
  }

  def updateFromProto(proto: MessageType, existing: ModelType): (ModelType, Boolean) = {
    update(updateModelEntry(proto, existing), existing)
  }

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
  def updateModelEntry(proto: MessageType, existing: ModelType): ModelType // = createModelEntry(proto)

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
  def findRecord(req: MessageType): Option[ModelType]

  /**
   * Find zero or more model entries given a message request
   * @param req   Message type descriptor of model entries
   * @return      List of model entries that much request
   */
  def findRecords(req: MessageType): List[ModelType]
}

trait EnvHolder extends QueuedEvaluation {
  var envOption: Option[RequestEnv] = None
  def env: RequestEnv = {
    envOption.get
  }
  def setEnv(s: RequestEnv) = {
    envOption = Some(s)
    queueInTransaction { envOption = None }
  }
}

/** 
 * Composed trait for the implementation of the service bridge of 
 * models (event buffering/publishing, subscribe requests)
 */
trait EventedServiceModel[MessageType <: GeneratedMessage, ModelType]
    extends ServiceEventBuffering[MessageType, ModelType]
    with ServiceEventPublishing[MessageType]
    with ServiceEventSubscribing[MessageType] { self: MessageModelConversion[MessageType, ModelType] =>
}

/**
 * Composed trait of model observing/event buffering component
 */
trait ServiceEventBuffering[MessageType <: GeneratedMessage, ModelType]
    extends EventQueueingObserver[MessageType, ModelType]
    with LinkedBufferedEvaluation { self: MessageModelConversion[MessageType, ModelType] =>
}

/**
 * Implementation of publishing events using a subscription handler
 */
trait ServiceEventPublishing[MessageType <: GeneratedMessage] {
  protected val subHandler: ServiceSubscriptionHandler

  protected def publishEvent(event: Envelope.Event, resp: MessageType, key: String): Unit = {
    subHandler.publish(event, resp, key)
  }
}

/**
 * Implementation of passing subscribe requests to the subscription handler with routing information
 */
trait ServiceEventSubscribing[MessageType] {
  def getRoutingKey(req: MessageType): String
  protected val subHandler: ServiceSubscriptionHandler

  /**
   * list of keys to bind to the sub handler with, models can override this
   * function to return multiple binding keys.
   */
  def getSubscribeKeys(req: MessageType): List[String] = getRoutingKey(req) :: Nil

  /**
   * Subscribe to model events
   *  @param req      Specifies events to be routed
   *  @param queue    Queue provided by the messaging system
   */
  def subscribe(req: MessageType, queue: String): Unit = {
    val keys = getSubscribeKeys(req)
    keys.foreach(subHandler.bind(queue, _))
  }
}

