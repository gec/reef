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
package org.totalgrid.reef.util

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.concurrent.{ MailBox, TIMEOUT }

/// This test shows that a receiveWithin that timesout causes the next message to be
/// lost and I believe it also leaks the closure but I haven't been able to confirm 
/// that. 
/// http://lampsvn.epfl.ch/trac/scala/browser/scala/tags/R_2_8_0_final/src/library/scala/concurrent/MailBox.scala
/// The source code seems to indicate the problem is the receiveWithin closure is not
/// removed from the list of applicable recievers when the timeout expires. 
@RunWith(classOf[JUnitRunner])
class MailBoxTests extends FunSuite {

  case class Test(i: Int)

  /// Checks that the next message in the mail box matches what we expect
  def checkRecieved(mail: MailBox, value: Int): Boolean = {
    mail.receiveWithin(1) {
      case Test(x) => return (x == value)
      case TIMEOUT => return false
    }
    return false
  }

  /// this block can only receive the timeout (not the message type!) this works
  /// as expected since it doesn't register as consuming the message of interest
  def checkTimeoutCantRecieve(mail: MailBox): Boolean = {
    mail.receiveWithin(1) {
      case TIMEOUT => return true
    }
    return false
  }

  /// this block can receive either a timeout or the message type (which is the
  /// way we would expect to see it used in real code). I have added a println
  /// which should show if this closure is still "alive" 
  def checkTimeoutCanReceive(mail: MailBox): Boolean = {
    mail.receiveWithin(1) {
      case Test(x) => {
        println("unexpected test value: " + x)
        return false
      }
      case TIMEOUT => return true
    }
    return false
  }

  test("Works with no timeouts") {
    val mail = new MailBox
    mail.send(new Test(1))
    mail.send(new Test(2))

    assert(checkRecieved(mail, 1))
    assert(checkRecieved(mail, 2))

    mail.send(new Test(3))
    assert(checkRecieved(mail, 3))
  }

  test("Passes with timeout that cant recieve") {
    val mail = new MailBox
    mail.send(new Test(1))
    assert(checkRecieved(mail, 1))

    assert(checkTimeoutCantRecieve(mail))

    mail.send(new Test(2))
    assert(checkRecieved(mail, 2))
  }

  test("Fails because timeout eats message") {
    val mail = new MailBox

    assert(checkTimeoutCanReceive(mail))

    // fails because the timeout test eats the message, should see message on stdout
    mail.send(new Test(2))
    assert(checkRecieved(mail, 2) == false)
  }

  test("Passes with sacrificial message") {
    val mail = new MailBox

    assert(checkTimeoutCanReceive(mail))

    // sending an extra message that is sent to the timeout closure fixes the issue
    mail.send(new Test(99))

    mail.send(new Test(2))
    assert(checkRecieved(mail, 2))
  }
}
