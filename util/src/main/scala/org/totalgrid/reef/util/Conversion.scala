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

object Conversion {

  implicit def convertAnyToOption[A](x: A): Option[A] = Option(x)

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
