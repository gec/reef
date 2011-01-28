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

/**
 * Provides a flexible "lazy var" implementation that allows code to specify
 * lazy attributes (which lazy val provides) but also allow us to set the value 
 * manually and "weakly" read the value ("weakly" means that we wont force it
 * to evaluate if it hasn't allready be read).
 *  
 * Lossley based on code from: 
 * http://scala-programming-language.1934581.n4.nabble.com/lazy-var-td1943810.html
 */
class LazyVar[T](init: => T) {
  private var thunk: (() => T) = { () => { thunk = { () => init }; thunk() } }
  private var thunked: Option[T] = None

  /**
   * gets the stored value, calculating it if necessary
   */
  def value = thunked match {
    case Some(t) => t
    case None => thunked = Some(thunk()); thunked.get
  }

  /**
   * sets the calculation for the next call to value
   */
  def value_=(newVal: () => T) = {
    thunk = { () => { thunk = { newVal }; thunk() } }
    thunked = None
  }

  /**
   * sets the value, doesn't require that value is called
   */
  def value_=(newVal: T) = {
    thunked = Some(newVal)
  }

  /**
   * Gets an option representing whether the value has been filled out or not. usefull
   * for code that wants to iterate over fields without forcing the values to be loaded:
   * Ex:
   * entity.attribute.asOption.map{value => println(value)}
   */
  def asOption = thunked match {
    case Some(t) => thunked
    case None => None
  }

  /**
   * alternate syntax, lv() -> lv.value
   */
  def apply() = value
  /**
   * alternate syntax, lv.! -> lv.value
   */
  def ! = value
  /**
   * alternate syntax, lv := T -> lv.value = T 
   */
  def :=(newVal: => T) = value_=(newVal)
  /**
   * alternate syntax, lv.? -> lv.asOption
   */
  def ? = asOption
}
object LazyVar {
  def apply[T](init: => T) = new LazyVar({ init })
}