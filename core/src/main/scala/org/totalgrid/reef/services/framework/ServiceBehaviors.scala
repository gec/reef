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

import org.totalgrid.reef.services.ServiceProviderHeaders._

object ServiceBehaviors {
  /**
   * Default REST "Get" behavior
   */
  trait GetEnabled extends HasSubscribe with ModeledService {
    def get(req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        val proto = model.findRecords(req).map(model.convertToProto(_))
        Response(Envelope.Status.OK, proto)
      }
    }
  }

  trait AsyncGetEnabled extends GetEnabled { self: ModeledService =>
    def getAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = {
      doAsyncGet(get(req, env), callback)
    }

    protected def doAsyncGet(rsp: Response[ServiceType], callback: Response[ServiceType] => Unit) = callback(rsp)
  }

  /**
   * PUTs and POSTs always create a new entry, there are no updates
   */

  trait PutOnlyCreates extends HasCreate with HasSubscribe with ModeledService {

    def put(req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      modelTrans.transaction { (model: ServiceModelType) =>
        model.setEnv(env)
        val (value, status) = create(model, req)
        env.subQueue.foreach(subscribe(model, value, _))
        Response(status, value :: Nil)
      }
    }

  }

  trait PostPartialUpdate extends HasUpdate with HasSubscribe with ModeledService {

    def post(req: ServiceType, env: RequestEnv): Response[ServiceType] = modelTrans.transaction { model =>
      model.setEnv(env)
      val (value, status) = model.findRecord(req) match {
        case Some(x) => update(model, req, x)
        case None => throw new BadRequestException("Record not found: " + req)
      }
      env.subQueue.foreach(subscribe(model, value, _))
      Response(status, value :: Nil)
    }

    override def preUpdate(proto: ServiceType, existing: ModelType): ServiceType = merge(proto, existing)

    protected def merge(req: ServiceType, current: ModelType): ServiceType

  }

  /**
   * Default REST "Put" behavior, currently accessed through both put and post verbs
   */
  trait PutEnabled extends HasCreate with HasUpdate with HasSubscribe with ModeledService {

    protected def doPut(req: ServiceType, env: RequestEnv, model: ServiceModelType): Response[ServiceType] = {
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

    def put(req: ServiceType, env: RequestEnv): Response[ServiceType] =
      modelTrans.transaction { doPut(req, env, _) }

  }

  trait AsyncPutEnabled extends PutEnabled { self: ModeledService =>

    def putAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      modelTrans.transaction { model => doAsyncPutPost(doPut(req, env, model), callback) }

    protected def doAsyncPutPost(rsp: Response[ServiceType], callback: Response[ServiceType] => Unit) = callback(rsp)
  }

  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled extends HasDelete with HasSubscribe with ModeledService {

    def delete(req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      modelTrans.transaction { model: ServiceModelType =>
        model.setEnv(env)
        env.subQueue.foreach(subscribe(model, req, _))
        val deleted = doDelete(model, req)
        Response(Envelope.Status.DELETED, deleted)
      }
    }

  }

  def noVerb(verb: Envelope.Verb) = throw new BadRequestException("Verb not implemented: " + verb)

  /**
   * Default service "Subscribe" behavior, must mix in GetEnabled
   */
  trait SubscribeEnabled { self: ModeledService =>
    def subscribe(model: ServiceModelType, req: ServiceType, queue: String) = model.subscribe(req, queue)
  }

  /**
   * REST "Get" disabled, throws an exception
   */
  trait GetDisabled { self: ModeledService =>
    def get(req: ServiceType, env: RequestEnv): Response[ServiceType] = noVerb(Envelope.Verb.GET)
  }

  /**
   * REST "Post" disabled, throws an exception
   */
  trait PostDisabled { self: ModeledService =>
    def post(req: ServiceType, env: RequestEnv): Response[ServiceType] = noVerb(Envelope.Verb.POST)
  }

  /**
   * REST "Put" disabled, throws an exception
   */
  trait PutDisabled { self: ModeledService =>
    def post(req: ServiceType, env: RequestEnv): Response[ServiceType] = noVerb(Envelope.Verb.POST)
    def put(req: ServiceType, env: RequestEnv): Response[ServiceType] = noVerb(Envelope.Verb.PUT)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait DeleteDisabled { self: ModeledService =>
    def delete(req: ServiceType, env: RequestEnv): Response[ServiceType] = noVerb(Envelope.Verb.DELETE)
    throw new BadRequestException("Delete not allowed")
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncDeleteDisabled { self: ModeledService =>
    def deleteAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = noVerb(Envelope.Verb.DELETE)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncGetDisabled { self: ModeledService =>
    def getAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = noVerb(Envelope.Verb.GET)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncPutDisabled { self: ModeledService =>
    def putAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = noVerb(Envelope.Verb.PUT)
  }

  /**
   * REST "Delete" disabled, throws an exception
   */
  trait AsyncPostDisabled { self: ModeledService =>
    def postAsync(req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = noVerb(Envelope.Verb.POST)
  }

  /**
   * REST "Subscribe" disabled, throws an exception
   */
  trait SubscribeDisabled { self: ModeledService =>
    def subscribe(model: ServiceModelType, req: ServiceType, queue: String) =
      throw new BadRequestException("Subscribe not allowed")
  }

}