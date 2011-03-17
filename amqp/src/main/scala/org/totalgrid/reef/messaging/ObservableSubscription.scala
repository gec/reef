/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging

/**
 * observer pattern for objects that only care about online/offline
 */
trait ObservableConnection {
  type ConnObserver = (Boolean) => Unit

  var queue: Option[String]

  def observeConnection(o: ConnObserver) = this.synchronized {
    o(queue.isDefined)
    connObservers = o :: connObservers
  }

  private var connObservers: List[ConnObserver] = Nil
  protected def onConnectEvent(online: Boolean) = this.synchronized {
    connObservers.foreach(f => f(online))
  }

}

/**
 * 	Describes an object that notify a set of observers when a queue changes state
 */
trait ObservableSubscription {

  type Observer = (Boolean, String) => Unit

  var queue: Option[String]

  def observe(o: Observer) = this.synchronized {
    o(queue.isDefined, queue.getOrElse(""))
    observers = o :: observers
  }

  //variation of observe that only cares about the online state
  def resubscribe(resubFunction: String => Unit) = {
    observe((online, queue) => if (online) resubFunction(queue))
  }

  private var observers: List[Observer] = Nil
  protected def onChange(online: Boolean, queue: String) = this.synchronized {
    observers.foreach(f => f(online, queue))
  }

}

trait ObserverableBrokerObject extends ObservableConnection with ObservableSubscription