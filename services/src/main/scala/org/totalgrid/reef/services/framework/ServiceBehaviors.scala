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
import org.totalgrid.reef.client.sapi.client.Response

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException

/**
 * implementations for common behaviors for the services that use a "model" object.
 *
 * NOTE: It is vital to make sure that contextSource.transaction has completed before
 * calling the service callback because this creates a race condition for the client.
 * In that case the client may receive the response before the SQL transaction has completed
 * and if it sends a new request immediately another SQL transaction could be started that wouldn't
 * see whatever changes we had just made but were not "committed" yet.
 */
object ServiceBehaviors {
  /**
   * Default REST "Get" behavior
   */
  trait GetEnabled extends HasRead with AuthorizesRead with HasSubscribe with HasModelFactory with AsyncContextRestGet {
    def get(contextSource: RequestContextSource, req: ServiceType): Response[ServiceType] = {
      contextSource.transaction { context =>
        val results = read(context, model, req)
        context.getHeaders.subQueue.foreach(subscribe(context, model, req, _))
        Response(Envelope.Status.OK, results)
      }
    }
    override def getAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {
      callback(get(contextSource, req))
    }
  }

  trait AsyncGetEnabled extends GetEnabled {

    override def getAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {
      doAsyncGet(get(contextSource, req), callback)
    }

    protected def doAsyncGet(rsp: Response[ServiceType], callback: Response[ServiceType] => Unit) = callback(rsp)
  }

  /**
   * POSTs create a new entry, there are no updates
   */

  trait PutOnlyCreates extends HasCreate with AuthorizesCreate with HasSubscribe with HasModelFactory with AsyncContextRestPut {

    def put(contextSource: RequestContextSource, req: ServiceType): Response[ServiceType] = {
      contextSource.transaction { context =>
        val (value, status) = create(context, model, req)
        context.getHeaders.subQueue.foreach(subscribe(context, model, value, _))
        Response(status, value :: Nil)
      }
    }

    override def putAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(contextSource, req))
  }

  trait PostPartialUpdate extends HasUpdate with AuthorizesUpdate with HasSubscribe with HasModelFactory with AsyncContextRestPost {

    def post(contextSource: RequestContextSource, req: ServiceType): Response[ServiceType] = {
      contextSource.transaction { context =>
        val (value, status) = model.findRecord(context, req) match {
          case Some(x) => update(context, model, req, x)
          case None => throw new BadRequestException("Record not found: " + req)
        }
        context.getHeaders.subQueue.foreach(subscribe(context, model, value, _))
        Response(status, value :: Nil)
      }
    }

    override def preUpdate(context: RequestContext, proto: ServiceType, existing: ModelType): ServiceType = merge(context, proto, existing)

    protected def merge(context: RequestContext, req: ServiceType, current: ModelType): ServiceType

    override def postAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(post(contextSource, req))
  }

  /**
   * Default REST "Put" behavior updates or creates
   */
  trait PutCreatesOrUpdates
      extends HasCreate with AuthorizesCreate
      with HasUpdate with AuthorizesUpdate
      with HasSubscribe
      with HasModelFactory
      with AsyncContextRestPut {

    protected def doPut(contextSource: RequestContextSource, req: ServiceType, model: ServiceModelType): Response[ServiceType] = {
      contextSource.transaction { context =>
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
        context.getHeaders.subQueue.foreach(subscribe(context, model, proto, _))
        Response(status, proto :: Nil)
      }
    }

    def put(contextSource: RequestContextSource, req: ServiceType): Response[ServiceType] = doPut(contextSource, req, model)

    override def putAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(put(contextSource, req))
  }

  trait AsyncPutCreatesOrUpdates extends PutCreatesOrUpdates with HasModelFactory with AsyncContextRestPut {

    override def putAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      doAsyncPutPost(contextSource, doPut(contextSource, req, model), callback)

    protected def doAsyncPutPost(contextSource: RequestContextSource, rsp: Response[ServiceType], callback: Response[ServiceType] => Unit)
  }

  /**
   * Default REST "Delete" behavior
   */
  trait DeleteEnabled extends HasDelete with AuthorizesDelete with HasSubscribe with HasModelFactory with AsyncContextRestDelete {

    def delete(contextSource: RequestContextSource, req: ServiceType): Response[ServiceType] = {
      contextSource.transaction { context =>
        context.getHeaders.subQueue.foreach(subscribe(context, model, req, _))
        val deleted = doDelete(context, model, req)
        val status = if (deleted.isEmpty) Envelope.Status.NOT_MODIFIED else Envelope.Status.DELETED
        Response(status, deleted)
      }
    }
    override def deleteAsync(contextSource: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit =
      callback(delete(contextSource, req))
  }

  /**
   * Basic subscribe implementation
   */
  trait SubscribeEnabled extends HasAllTypes with HasSubscribe {

    override def subscribe(context: RequestContext, model: ServiceModelType, req: ServiceType, queue: String) = model.subscribe(context, req, queue)
  }

}

