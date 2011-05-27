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
package org.totalgrid.reef.util

import scala.collection.immutable
import scala.annotation.tailrec

class Times(num: Int) {
  assert(num >= 0)

  def times(f: => Unit): Unit = count(_ => f)

  def count(f: Int => Unit): Unit = {
    @tailrec
    def times(i: Int): Unit = if (i <= num) { f(i); times(i + 1) }
    if (num > 0) times(1)
  }
}

class Mapified[A](i: Iterable[A]) {

  def mapify[B](keygen: A => B): immutable.Map[B, A] = {
    i.foldLeft(immutable.Map.empty[B, A]) { (sum, x) =>
      sum + (keygen(x) -> x)
    }
  }
}

class TakeRand[A](l: List[A]) {

  /**
   * @return an Option with either a random element from the list or None if list is empty
   */
  def takeRand: Option[A] = {
    if (l.size == 0) None
    else Some(l.apply(new scala.util.Random().nextInt(l.size)))
  }
}

object Conversion {

  implicit def convertIntToTimes(num: Int): Times = new Times(num)
  implicit def convertIterableToMapified[A](i: Iterable[A]) = new Mapified(i)
  implicit def convertAnyToOption[A <: Any](x: A): Option[A] = Option(x)
  implicit def convertListToRandList[A](l: List[A]): TakeRand[A] = new TakeRand[A](l)

  /**
   * takes a string and tries to cast it to long, double, boolean or returns the string
   * if none of those conversions work
   */
  def convertStringToType(s: String): Any = {
    import Unappliers._
    s match {
      case Int(x) => x
      case Long(x) => x
      case Double(x) => x
      case Boolean(x) => x
      case _ => s
    }
  }

}

object Unappliers {
  object Int {
    def unapply(s: String): Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }
  object Boolean {
    def unapply(s: String): Option[Boolean] = try {
      Some(s.toBoolean)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }
  object Double {
    def unapply(s: String): Option[Double] = try {
      Some(s.toDouble)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }
  object Long {
    def unapply(s: String): Option[Long] = try {
      Some(s.toLong)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }
}
