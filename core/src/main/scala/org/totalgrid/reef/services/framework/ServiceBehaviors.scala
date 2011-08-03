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
    def get(context: RequestContext, req: ServiceType): Response[ServiceType] = {
      val results = read(context, model, req)
      context.headers.subQueue.foreach(subscribe(context, model, req, _))
      Response(Envelope.Status.OK, results)
    }
    override def getAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {
      callback(get(context, req))
    }
  }

  trait AsyncGetEnabled extends GetEnabled {

    override def getAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {
      doAsyncGet(get(context, req), callback)
    }

    protected def doAsyncGet(rsp: Response[ServiceType], callback: Response[ServiceType] => Unit) = callback(rsp)
  }

  /**
   * POSTs create a new entry, there are no updates
   */

  trait PutOnlyCreates extends HasCreate with AuthorizesCreate with HasSubscribe with HasServiceTransactable with AsyncContextRestPut {

    def put(context: RequestContext, req: ServiceType): Response[ServiceType] = {
      val (value, status) = create(context, model, req)
      context.headers.subQueue.foreach(subscribe(context, model, value, _))
      Response(status, value :: Nil)
    }

    override def putAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(context, req))
  }

  trait PostPartialUpdate extends HasUpdate with AuthorizesUpdate with HasSubscribe with HasServiceTransactable with AsyncContextRestPost {

    def post(context: RequestContext, req: ServiceType): Response[ServiceType] = {
      val (value, status) = model.findRecord(context, req) match {
        case Some(x) => update(context, model, req, x)
        case None => throw new BadRequestException("Record not found: " + req)
      }
      context.headers.subQueue.foreach(subscribe(context, model, value, _))
      Response(status, value :: Nil)
    }

    override def preUpdate(context: RequestContext, proto: ServiceType, existing: ModelType): ServiceType = merge(context, proto, existing)

    protected def merge(context: RequestContext, req: ServiceType, current: ModelType): ServiceType

    override def postAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(post(context, req))
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

    protected def doPut(context: RequestContext, req: ServiceType, model: ServiceModelType): Response[ServiceType] = {
      val (proto, status) = try {
        model.findRecord(context, req) match {
          case None => create(context, model, req)
          case Some(x) => update(context, model, req, x)
        }
      } catch {
        // some items can be created without having uniquely identifying fields
        // so may have no search terms to look for
        // TODO: evaluate replacing NoSearchTermsException with flags
        case e: NoSearchTermsException => create(context, model, req)
      }
      context.headers.subQueue.foreach(subscribe(context, model, proto, _))
      Response(status, proto :: Nil)
    }

    def put(context: RequestContext, req: ServiceType): Response[ServiceType] = doPut(context, req, model)

    override def putAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(context, req))
  }

  trait AsyncPutCreatesOrUpdates extends PutCreatesOrUpdates with HasServiceTransactable with AsyncContextRestPut {

    override def putAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      doAsyncPutPost(context, doPut(context, req, model), callback)

    protected def doAsyncPutPost(context: RequestContext, rsp: Response[ServiceType], callback: Response[ServiceType] => Unit)
  }

  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled extends HasDelete with AuthorizesDelete with HasSubscribe with HasServiceTransactable with AsyncContextRestDelete {

    def delete(context: RequestContext, req: ServiceType): Response[ServiceType] = {
      context.headers.subQueue.foreach(subscribe(context, model, req, _))
      val deleted = doDelete(context, model, req)
      val status = if (deleted.isEmpty) Envelope.Status.NOT_MODIFIED else Envelope.Status.DELETED
      Response(status, deleted)
    }
    override def deleteAsync(context: RequestContext, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(delete(context, req))
  }

  /**
   * Basic subscribe implementation
   */
  trait SubscribeEnabled extends HasAllTypes with HasSubscribe {

    override def subscribe(context: RequestContext, model: ServiceModelType, req: ServiceType, queue: String) = model.subscribe(context, req, queue)
  }

}

