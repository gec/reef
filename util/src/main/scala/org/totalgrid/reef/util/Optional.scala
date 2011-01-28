/**
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

object Optional {

  /**
   * calls the has (or all of the has) functions, taking advantage of short circuiting, to return None if any of the has functions
   * fail or the result of the get function wrapped in Some.
   */
  def hasGet[T](has: => Boolean, get: => T): Option[T] = if (has) Some(get) else None
  def hasGet[T](has: => Boolean, has1: => Boolean, get: => T): Option[T] = if (has && has1) Some(get) else None
  def hasGet[T](has: => Boolean, has1: => Boolean, has2: => Boolean, get: => T): Option[T] = if (has && has1 && has2) Some(get) else None

  // if (isTrue) Some(obj) else None   --->    isTrue thenGet obj | isTrue ? obj
  implicit def boolWrapper(b: Boolean) = new OptionalBoolean(b)
  class OptionalBoolean(b: Boolean) {
    def ?[T](t: T): Option[T] = thenGet(t) // Precedence different
    def thenGet[T](t: => T): Option[T] = if (b) Some(t) else None
  }

  // Fall-through/flattening for Option
  // Experimental -- not sure if this is going to conflict with collections, alternately control flow with Option might
  // be a bad idea to start with.
  //
  // You would never intentionally do this, but it arises in optional control flow
  //
  // var optOpt : Optional[Optional[Int]] = Some(Some(5))
  // optOpt.get.map(println(_)) (WORKS)     --->     optOpt.flatten.map(println(_)) (WORKS)
  // optOpt = None
  // optOpt.get.map(println(_)) (ERROR)     --->     optOpt.flatten.map(println(_)) (WORKS)
  //
  class OptFlatten[A](opt: Option[Option[A]]) {
    def flatten = opt getOrElse None
  }
  implicit def opt2optFlatten[A](opt: Option[Option[A]]) = new OptFlatten[A](opt)

  // Implicit wrappers for protos/others that convert has/get to optional and preserve responsivity
  // at any depth (you can look up Alarm -> ContextInfo -> Attr even if Alarm has no ContextInfo)
  trait Optional[T] { def getOption: Option[T] }
  implicit def optAttrToOpt[T](optional: Optional[T]): Option[T] = optional.getOption

  class OptionalStruct[A](myself: Option[A]) extends Optional[A] {
    def getOption = myself
    def optionally[B](get: A => B): Option[B] = {
      myself.map(me => get(me))
    }
    def optionally[B](has: A => Boolean, get: A => B): Option[B] = {
      myself.flatMap(me => has(me) thenGet get(me))
    }
  }
}