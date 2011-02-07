/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.util.Conversion.convertIterableToMapified

/**
 * mixin that handles the add/remove/modify/subscribed behavior by keeping protos in a map and freeing up the 
 * user code to handle only the "payload" add/remove behavior. User code can therefore skip any checks to make
 * sure that the object isn't allready created/removed.
 * 
 * This class can be mixed in to implement the abstract functions of ServiceContext[T]
 */
trait KeyedMap[T] extends Logging {

  def hasChangedEnoughForReload(updated: T, existing: T): Boolean

  def getKey(value: T): String

  /**
   * called when a new entry is added to the map
   */
  def addEntry(c: T)
  /**
   * called when an entry is deleted from the map
   */
  def removeEntry(c: T)

  /* ----- Mutable state -----  */
  private var active = Map.empty[String, T] //active connections

  /**    Load a list of slave device connections
   */
  def subscribed(list: List[T]): Unit = {
    val map = list.mapify { x => getKey(x) }
    active.values.foreach { c =>
      map.get(getKey(c)) match {
        case Some(x) => remove(c)
        case None =>
      }
    }

    list.foreach { c =>
      active.get(getKey(c)) match {
        case Some(x) => modify(c)
        case None => add(c)
      }
    }
  }

  /* --  Handlers for device connections --*/
  def add(c: T): Unit = {
    active.get(getKey(c)) match {
      case Some(x) => modify(c)
      case None =>
        addEntry(c)
        active += getKey(c) -> c
        info { "added key " + getKey(c) }
    }

  }

  def remove(c: T): Unit = {
    active.get(getKey(c)) match {
      case None => warn { "Remove on unregistered key: " + getKey(c) }
      case Some(x) =>
        removeEntry(x)
        active -= getKey(c)
        info { "removed ... " + getKey(c) }
    }
  }

  def modify(c: T): Unit = {
    active.get(getKey(c)) match {
      case Some(x) =>
        if (hasChangedEnoughForReload(c, x)) {
          remove(c)
          add(c)
        }
      case None => add(c)
    }
  }

  def clear() = active.values.foreach(v => remove(v))

}
