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

import org.totalgrid.reef.services.ProtoServiceEndpoint
import org.totalgrid.reef.messaging.ProtoServiceable
import org.totalgrid.reef.protoapi.{ ProtoServiceException, RequestEnv }
import org.totalgrid.reef.protoapi.ProtoServiceTypes.Response
import org.totalgrid.reef.proto.Envelope

import org.totalgrid.reef.services.ServiceProviderHeaders._

/**
 * Hooks/callbacks for service implementations to modify standard REST behavior
 */
trait ServiceHooks { self: ProtoServiceTypes =>

  /**
   * Called before create
   *  @param proto  Create request message
   *  @return       Verified/modified create request message
   */
  protected def preCreate(proto: ProtoType): ProtoType = proto

  /**
   * Called after successful create
   *  @param proto  Created response message
   */
  protected def postCreate(created: ModelType, request: ProtoType): Unit = {}

  /**
   * Called before update
   *  @param proto      Update request message
   *  @param existing   Existing model entry
   *  @return           Verified/modified update request message
   */
  protected def preUpdate(proto: ProtoType, existing: ModelType): ProtoType = proto

  /**
   * Called after successful update
   *  @param proto Updated response message
   */
  protected def postUpdate(updated: ModelType, request: ProtoType): Unit = {}

  /**
   * Called before delete
   *  @param proto    Delete request message
   */
  protected def preDelete(proto: ProtoType): Unit = {}

  /**
   * Called after successful delete
   *  @param proto    Deleted objects
   */
  protected def postDelete(proto: List[ProtoType]): Unit = {}
}

/**
 * Common types needed by generic service implementations
 */
trait ProtoServiceTypes {
  type ProtoType <: GeneratedMessage
  type ModelType
  type ServiceModelType <: ServiceModel[ProtoType, ModelType]
}

/**
 * Shared dependencies for generic service implementations
 */
trait ProtoServiceShared extends ProtoServiceTypes with ServiceHooks {
  protected val modelTrans: ServiceTransactable[ServiceModelType]

  def subscribe(model: ServiceModelType, req: ProtoType, queue: String)
}

/**
 * Contains implementations of BaseProtoService operations put, post, get, delete, subscribe
 */
object BaseProtoService {
  /**
   * Default REST "Get" behavior
   */
  trait GetEnabled { self: ProtoServiceShared =>

    def get(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        val proto = model.findRecords(req).map(model.convertToProto(_))
        new Response(Envelope.Status.OK, proto)
      }
    }
  }

  /**
   * PUTs and POSTs always create a new entry, there are no updates
   */
  trait PostLikeEnabled { self: ProtoServiceShared =>

    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = put(req, env)

    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        val proto = create(model, req)
        env.subQueue.foreach(subscribe(model, proto, _))
        new Response(Envelope.Status.CREATED, proto)
      }
    }

    // Create and update implementations
    protected def create(model: ServiceModelType, req: ProtoType): ProtoType = {
      val proto = preCreate(req)
      val sql = model.createFromProto(proto)
      postCreate(sql, proto)
      model.convertToProto(sql)
    }
  }

  /**
   * Default REST "Put" behavior, currently accessed through both put and post verbs
   */
  trait PutPostEnabled { self: ProtoServiceShared =>

    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = put(req, env)

    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        val (proto, status) = model.findRecord(req) match {
          case None => create(model, req)
          case Some(x) => update(model, req, x)
        }
        env.subQueue.foreach(subscribe(model, proto, _))
        new Response(status, proto :: Nil)
      }
    }
    // Create and update implementations
    protected def create(model: ServiceModelType, req: ProtoType): Tuple2[ProtoType, Envelope.Status] = {
      val proto = preCreate(req)
      val sql = model.createFromProto(req)
      postCreate(sql, req)
      (model.convertToProto(sql), Envelope.Status.CREATED)
    }

    // Found an existing record. Update it. 
    protected def update(model: ServiceModelType, req: ProtoType, existing: ModelType): Tuple2[ProtoType, Envelope.Status] = {
      val proto = preUpdate(req, existing)
      val (sql, updated) = model.updateFromProto(req, existing)
      postUpdate(sql, req)
      val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
      (model.convertToProto(sql), status)
    }
  }
  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled { self: ProtoServiceShared =>

    def delete(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        preDelete(req)
        val deleted = doDelete(model, req)
        postDelete(deleted)
        new Response(Envelope.Status.DELETED, deleted)
      }
    }

    protected def doDelete(model: ServiceModelType, req: ProtoType): List[ProtoType] = {
      // TODO: consider stripping off everything but UID if UID set on delete
      val existing = model.findRecords(req)
      existing.foreach(model.delete(_))
      existing.map(model.convertToProto(_))
    }
  }

  /**
   * Default service "Subscribe" behavior, must mix in GetEnabled
   */
  trait SubscribeEnabled { self: GetEnabled with ProtoServiceShared =>

    def subscribe(model: ServiceModelType, req: ProtoType, queue: String) {
      model.subscribe(req, queue)
    }
  }

  /**
   * REST "Get" disabled, throws an exception
   */
  trait GetDisabled { self: ProtoServiceShared =>
    def get(req: ProtoType, env: RequestEnv): Response[ProtoType] =
      throw new ProtoServiceException("Get not implemented")
  }

  /**
   * REST "Post" disabled, throws an exception
   */
  trait PostDisabled { self: ProtoServiceShared =>
    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] =
      throw new ProtoServiceException("Post not allowed")
  }

  /**
   * REST "Put" disabled, throws an exception
   */
  trait PutDisabled { self: ProtoServiceShared =>
    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = put(req, env)
    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] =
      throw new ProtoServiceException("Put not allowed")
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait DeleteDisabled { self: ProtoServiceShared =>
    def delete(req: ProtoType, env: RequestEnv): Response[ProtoType] =
      throw new ProtoServiceException("Delete not allowed")
  }

  /**
   * REST "Subscribe" disabled, throws an exception
   */
  trait SubscribeDisabled { self: ProtoServiceShared =>
    def subscribe(model: ServiceModelType, req: ProtoType, queue: String) {
      throw new ProtoServiceException("Subscribe not allowed")
    }
  }
}

/**
 * Base class for services which handle protobuf messages and act on service models.
 * 
 * Implements ProtoServiceable/ProtoServiceEndpoint interfaces to the messaging system
 * and provides shared types/resource definitions for mixed-in service behavior.
 */
trait BaseProtoService[PT <: GeneratedMessage, MT, SMT <: ServiceModel[PT, MT]]
    extends ProtoServiceShared
    with ProtoServiceable[PT]
    with ProtoServiceEndpoint {

  type ProtoType = PT
  type ModelType = MT
  type ServiceModelType = SMT
}

import BaseProtoService._

/**
 * Common super type for fully RESTful proto services
 */
trait BasicProtoService[ProtoType <: GeneratedMessage, ModelType, ServiceModelType <: ServiceModel[ProtoType, ModelType]]
  extends BaseProtoService[ProtoType, ModelType, ServiceModelType]
  with GetEnabled
  with SubscribeEnabled
  with PutPostEnabled
  with DeleteEnabled

