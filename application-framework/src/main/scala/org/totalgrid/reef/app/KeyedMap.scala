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
package org.totalgrid.reef.app

import com.weiglewilczek.slf4s.Logging

/**
 * simple interface that KeyedMap implements to hide implementation details from consumers
 */
trait ClearableMap[A] {
  def add(obj: A)
  def modify(obj: A)
  def remove(obj: A)

  def clear()
}

/**
 * mixin that handles the add/remove/modify/subscribed behavior by keeping protos in a map and freeing up the
 * user code to handle only the "payload" add/remove behavior. User code can therefore skip any checks to make
 * sure that the object isn't already created/removed.
 */
trait KeyedMap[A] extends ClearableMap[A] with Logging {

  protected def hasChangedEnoughForReload(updated: A, existing: A): Boolean

  protected def getKey(value: A): String

  /**
   * called when a new entry is added to the map
   */
  def addEntry(c: A)
  /**
   * called when an entry is deleted from the map
   */
  def removeEntry(c: A)

  /* ----- Mutable state -----  */
  private var active = Map.empty[String, A]

  /* --  Handlers for device connections --*/
  def add(connection: A) {
    active.get(getKey(connection)) match {
      case Some(x) => modify(connection)
      case None =>
        logger.info("adding key " + getKey(connection))
        addEntry(connection)
        active += getKey(connection) -> connection
        logger.info("added key " + getKey(connection))
    }

  }

  def remove(connection: A) {
    active.get(getKey(connection)) match {
      case None => logger.info("Remove on unregistered key: " + getKey(connection))
      case Some(x) =>
        logger.info("removing key: " + getKey(connection))
        removeEntry(x)
        active -= getKey(connection)
        logger.info("removed key: " + getKey(connection))
    }
  }

  def modify(connection: A) {
    active.get(getKey(connection)) match {
      case Some(x) =>
        if (hasChangedEnoughForReload(connection, x)) {
          remove(connection)
          add(connection)
        }
      case None => add(connection)
    }
  }

  def clear() = active.values.foreach(v => remove(v))

}
