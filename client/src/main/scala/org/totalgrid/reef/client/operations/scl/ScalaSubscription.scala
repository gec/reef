package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.{ SubscriptionEventAcceptor, SubscriptionEvent, Subscription }
import org.totalgrid.reef.client.proto.Envelope

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

trait ScalaSubscription {

  class RichSubscription[A](sub: Subscription[A]) {

    /*def start(f: (SubscriptionEvent[A]) => Unit) {
      sub.start(new SubscriptionEventAcceptor[A] {
        def onEvent(event: SubscriptionEvent[A]) { f(event)}
      })
    }*/
    def onEvent(f: Event[A] => Unit) {
      sub.start(new SubscriptionEventAcceptor[A] {
        def onEvent(event: SubscriptionEvent[A]) { f(Event(event.getEventType, event.getValue)) }
      })
    }
  }

  implicit def implicitSub[A](sub: Subscription[A]): RichSubscription[A] = new RichSubscription(sub)
}

object ScalaSubscription extends ScalaSubscription

case class Event[A](event: Envelope.SubscriptionEventType, value: A) extends SubscriptionEvent[A] {

  final override def getEventType() = event
  final override def getValue() = value
}