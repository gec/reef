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

import org.totalgrid.reef.services.framework.SquerylModel.NoSearchTermsException
import org.totalgrid.reef.sapi.client.Response

import org.totalgrid.reef.japi.{ BadRequestException, Envelope }
import org.totalgrid.reef.sapi.RequestEnv

import org.totalgrid.reef.sapi.service._

object ServiceBehaviors {
  /**
   * Default REST "Get" behavior
   */
  trait GetEnabled extends HasRead with AuthorizesRead with HasSubscribe with HasServiceTransactable with AsyncContextRestGet {
    def get(context: RequestContext, req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { model: ServiceModelType =>
          val results = read(context, model, req, env)
          env.subQueue.foreach(subscribe(model, req, _))
          Response(Envelope.Status.OK, results)
        }
      })
    }
    override def getAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = {
      val context = new SimpleRequestContext
      callback(get(context, req, env))
    }
  }

  trait AsyncGetEnabled extends GetEnabled {

    override def getAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = {
      val context = new SimpleRequestContext
      doAsyncGet(get(context, req, env), callback)
    }

    protected def doAsyncGet(rsp: Response[ServiceType], callback: Response[ServiceType] => Unit) = callback(rsp)
  }

  /**
   * POSTs create a new entry, there are no updates
   */

  trait PutOnlyCreates extends HasCreate with AuthorizesCreate with HasSubscribe with HasServiceTransactable with AsyncContextRestPut {

    def put(context: RequestContext, req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { model: ServiceModelType =>
          val (value, status) = create(context, model, req, env)
          env.subQueue.foreach(subscribe(model, value, _))
          Response(status, value :: Nil)
        }
      })
    }

    override def putAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(context, req, env))
  }

  trait PostPartialUpdate extends HasUpdate with AuthorizesUpdate with HasSubscribe with HasServiceTransactable with AsyncContextRestPost {

    def post(context: RequestContext, req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { model =>
          val (value, status) = model.findRecord(context, req) match {
            case Some(x) => update(context, model, req, x, env)
            case None => throw new BadRequestException("Record not found: " + req)
          }
          env.subQueue.foreach(subscribe(model, value, _))
          Response(status, value :: Nil)
        }
      })
    }

    override def preUpdate(context: RequestContext, proto: ServiceType, existing: ModelType, headers: RequestEnv): ServiceType = merge(context, proto, existing)

    protected def merge(context: RequestContext, req: ServiceType, current: ModelType): ServiceType

    override def postAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      callback(post(context, req, env))
  }

  /**
   * Default REST "Put" behavior updates or creates
   */
  trait PutCreatesOrUpdates
      extends HasCreate with AuthorizesCreate
      with HasUpdate with AuthorizesUpdate
      with HasSubscribe
      with HasServiceTransactable
      with AsyncContextRestPut {

    protected def doPut(context: RequestContext, req: ServiceType, env: RequestEnv, model: ServiceModelType): Response[ServiceType] = {
      val (proto, status) = try {
        model.findRecord(context, req) match {
          case None => create(context, model, req, env)
          case Some(x) => update(context, model, req, x, env)
        }
      } catch {
        // some items can be created without having uniquely identifying fields
        // so may have no search terms to look for
        // TODO: evaluate replacing NoSearchTermsException with flags
        case e: NoSearchTermsException => create(context, model, req, env)
      }
      env.subQueue.foreach(subscribe(model, proto, _))
      Response(status, proto :: Nil)
    }

    def put(context: RequestContext, req: ServiceType, env: RequestEnv): Response[ServiceType] =
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { doPut(context, req, env, _) }
      })

    override def putAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(context, req, env))
  }

  trait AsyncPutCreatesOrUpdates extends PutCreatesOrUpdates with HasServiceTransactable with AsyncContextRestPut {

    override def putAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { model => doAsyncPutPost(context, doPut(context, req, env, model), callback) }
      })

    protected def doAsyncPutPost(context: RequestContext, rsp: Response[ServiceType], callback: Response[ServiceType] => Unit)
  }

  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled extends HasDelete with AuthorizesDelete with HasSubscribe with HasServiceTransactable with AsyncContextRestDelete {

    def delete(context: RequestContext, req: ServiceType, env: RequestEnv): Response[ServiceType] = {
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        modelTrans.transaction { model: ServiceModelType =>
          env.subQueue.foreach(subscribe(model, req, _))
          val deleted = doDelete(context, model, req, env)
          val status = if (deleted.isEmpty) Envelope.Status.NOT_MODIFIED else Envelope.Status.DELETED
          Response(status, deleted)
        }
      })
    }
    override def deleteAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit =
      callback(delete(context, req, env))
  }

  /**
   * Basic subscribe implementation
   */
  trait SubscribeEnabled extends HasAllTypes with HasSubscribe {

    override def subscribe(model: ServiceModelType, req: ServiceType, queue: String) = model.subscribe(req, queue)
  }

}

