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

package org.totalgrid.reef.executor

import akka.actor.Actor._

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import akka.actor.{ ActorRef, Actor }

import org.totalgrid.reef.util.Conversion._
import org.totalgrid.reef.util.SyncVar

case class ExecuteFunc(fun: () => Unit)

class TestActor extends Actor {

  def receive = {
    case ExecuteFunc(fun) => fun()
  }
}

class HasExecute(a: ActorRef) {
  def execute(fun: => Unit) = a ! ExecuteFunc(() => fun)
}

@RunWith(classOf[JUnitRunner])
class AkkaActorTest extends FunSuite {

  def fixture[U](fun: HasExecute => U) {
    val a = actorOf[TestActor]
    val exe = new HasExecute(a)
    a.start()
    try {
      fun(exe)
    } finally {
      a.stop()
    }
  }

  test("Sending a message") {
    val numIncrements = 1000

    fixture { a =>
      val num = new SyncVar[Int](0)
      numIncrements.times(a.execute(num.atomic(_ + 1)))
      num.waitFor(_ == numIncrements)
    }
  }

}

