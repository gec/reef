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

import org.totalgrid.reef.services.framework.SquerylModel.NoSearchTermsException
import org.totalgrid.reef.api.ServiceTypes.Response

import org.totalgrid.reef.api.{ RequestEnv, BadRequestException, Envelope }
import org.totalgrid.reef.api.service.AsyncServiceBase

import org.totalgrid.reef.services.ServiceProviderHeaders._

object ServiceBehaviors {
  /**
   * Default REST "Get" behavior
   */
  trait GetEnabled { self: ModeledService =>
    def get(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        val proto = model.findRecords(req).map(model.convertToProto(_))
        Response(Envelope.Status.OK, proto)
      }
    }
  }

  trait AsyncGetEnabled extends GetEnabled { self: ModeledService =>
    def getAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit = {
      doAsyncGet(get(req, env), callback)
    }

    protected def doAsyncGet(rsp: Response[ProtoType], callback: Response[ProtoType] => Unit) = callback(rsp)
  }

  /**
   * PUTs and POSTs always create a new entry, there are no updates
   */
  trait PutOnlyCreates { self: ModeledService =>

    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        val proto = create(model, req)
        env.subQueue.foreach(subscribe(model, proto, _))
        Response(Envelope.Status.CREATED, proto :: Nil)
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

  trait PostPartialUpdate extends HasUpdate { self: ModeledService =>

    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = modelTrans.transaction { model =>
      model.setEnv(env)
      val (proto, status) = model.findRecord(req) match {
        case Some(x) => update(model, req, x)
        case None => throw new BadRequestException("Record not found: " + req)
      }
      env.subQueue.foreach(subscribe(model, proto, _))
      Response(status, proto :: Nil)
    }

    override def preUpdate(proto: ProtoType, existing: ModelType): ProtoType = merge(proto, existing)

    protected def merge(req: ProtoType, current: ModelType): ProtoType

  }

  /**
   * Default REST "Put" behavior, currently accessed through both put and post verbs
   */
  trait PutEnabled extends HasCreate with HasUpdate { self: ModeledService =>

    protected def doPut(req: ProtoType, env: RequestEnv, model: ServiceModelType): Response[ProtoType] = {
      model.setEnv(env)
      val (proto, status) = try {
        model.findRecord(req) match {
          case None => create(model, req)
          case Some(x) => update(model, req, x)
        }
      } catch {
        // some items can be created without having uniquely identifying fields
        // so may have no search terms to look for
        // TODO: evaluate replacing NoSearchTermsException with flags
        case e: NoSearchTermsException => create(model, req)
      }
      env.subQueue.foreach(subscribe(model, proto, _))
      Response(status, proto :: Nil)
    }

    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] =
      modelTrans.transaction { doPut(req, env, _) }

  }

  trait HasCreate { self: ModeledService =>

    protected def create(model: ServiceModelType, req: ProtoType): Tuple2[ProtoType, Envelope.Status] = {
      val proto = preCreate(req)
      val sql = model.createFromProto(req)
      postCreate(sql, req)
      (model.convertToProto(sql), Envelope.Status.CREATED)
    }

  }

  trait HasUpdate { self: ModeledService =>

    // Found an existing record. Update it.
    protected def update(model: ServiceModelType, req: ProtoType, existing: ModelType): Tuple2[ProtoType, Envelope.Status] = {
      val proto = preUpdate(req, existing)
      val (sql, updated) = model.updateFromProto(req, existing)
      postUpdate(sql, req)
      val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
      (model.convertToProto(sql), status)
    }
  }

  trait AsyncPutEnabled extends PutEnabled { self: ModeledService =>

    def putAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit =
      modelTrans.transaction { model => doAsyncPutPost(doPut(req, env, model), callback) }

    protected def doAsyncPutPost(rsp: Response[ProtoType], callback: Response[ProtoType] => Unit) = callback(rsp)
  }

  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled { self: ModeledService =>

    def delete(req: ProtoType, env: RequestEnv): Response[ProtoType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        preDelete(req)
        val deleted = doDelete(model, req)
        postDelete(deleted)
        Response(Envelope.Status.DELETED, deleted)
      }
    }

    protected def doDelete(model: ServiceModelType, req: ProtoType): List[ProtoType] = {
      // TODO: consider stripping off everything but UID if UID set on delete
      val existing = model.findRecords(req)
      existing.foreach(model.delete(_))
      existing.map(model.convertToProto(_))
    }
  }

  def noVerb(verb: Envelope.Verb) = throw new BadRequestException("Verb not implemented: " + verb)

  /**
   * Default service "Subscribe" behavior, must mix in GetEnabled
   */
  trait SubscribeEnabled { self: ModeledService =>
    def subscribe(model: ServiceModelType, req: ProtoType, queue: String) = model.subscribe(req, queue)
  }

  /**
   * REST "Get" disabled, throws an exception
   */
  trait GetDisabled { self: ModeledService =>
    def get(req: ProtoType, env: RequestEnv): Response[ProtoType] = noVerb(Envelope.Verb.GET)
  }

  /**
   * REST "Post" disabled, throws an exception
   */
  trait PostDisabled { self: ModeledService =>
    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = noVerb(Envelope.Verb.POST)
  }

  /**
   * REST "Put" disabled, throws an exception
   */
  trait PutDisabled { self: ModeledService =>
    def post(req: ProtoType, env: RequestEnv): Response[ProtoType] = noVerb(Envelope.Verb.POST)
    def put(req: ProtoType, env: RequestEnv): Response[ProtoType] = noVerb(Envelope.Verb.PUT)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait DeleteDisabled { self: ModeledService =>
    def delete(req: ProtoType, env: RequestEnv): Response[ProtoType] = noVerb(Envelope.Verb.DELETE)
    throw new BadRequestException("Delete not allowed")
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncDeleteDisabled { self: ModeledService =>
    def deleteAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit = noVerb(Envelope.Verb.DELETE)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncGetDisabled { self: ModeledService =>
    def getAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit = noVerb(Envelope.Verb.GET)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncPutDisabled { self: ModeledService =>
    def putAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit = noVerb(Envelope.Verb.PUT)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncPostDisabled { self: ModeledService =>
    def postAsync(req: ProtoType, env: RequestEnv)(callback: Response[ProtoType] => Unit): Unit = noVerb(Envelope.Verb.POST)
  }

  /**
   * REST "Subscribe" disabled, throws an exception
   */
  trait SubscribeDisabled { self: ModeledService =>
    def subscribe(model: ServiceModelType, req: ProtoType, queue: String) =
      throw new BadRequestException("Subscribe not allowed")
  }

}