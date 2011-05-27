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
package org.totalgrid.reef.protocol.dnp3

import scala.collection.mutable
import scala.actors.Actor

import concurrent.MailBox

case class StartMsg()
case class EndMsg()
case class PubMsg(pub: CountingMockPublisher, num: Int)
case class SetupPub(pub: CountingMockPublisher)
case class MinMsg(min: Int)

class CountingPublisherActor extends Actor {

  val map = mutable.Map.empty[CountingMockPublisher, Int]

  val mail = new MailBox //mail box we'll wait on

  start

  def addPub: IDataObserver = {
    val pub = new CountingMockPublisher(this)
    this ! SetupPub(pub)
    pub
  }

  def waitForMinMessages(min: Int, wait: Long): Boolean = {
    val num = getMin(wait)
    if (num < 0) false else {
      if (num >= min) true else waitForMinMessages(min, wait)
    }
  }

  protected def getMin(wait: Long): Int = {
    mail.receiveWithin(wait) {
      case MinMsg(x) => x
      case _ => -1
    }
  }

  def act {
    loop {
      react {
        case PubMsg(mock, num) => map.put(mock, num)
        case EndMsg => mail send MinMsg(min) //whenever we get a stop, update the min
        case SetupPub(mock) => map.put(mock, 0)
      }
    }
  }

  private def min: Int = map.values.foldLeft(Int.MaxValue) { (x, y) => if (y < x) y else x }

}

class CountingMockPublisher(a: CountingPublisherActor) extends IDataObserver {

  private var num = 0

  private def incr(): Int = {
    num += 1
    num
  }

  override def _Update(v: Analog, index: Long) = a ! PubMsg(this, incr)
  override def _Update(v: Binary, index: Long) = a ! PubMsg(this, incr)
  override def _Update(v: ControlStatus, index: Long) = a ! PubMsg(this, incr)
  override def _Update(v: Counter, index: Long) = a ! PubMsg(this, incr)
  override def _Update(v: SetpointStatus, index: Long) = a ! PubMsg(this, incr)

  override def _End() { a ! EndMsg }
  override def _Start() { a ! StartMsg }
}

