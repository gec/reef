/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measproc

import org.totalgrid.reef.persistence.ObjectCache
import scala.collection.mutable

class MockObjectCache[A] extends ObjectCache[A] {

  val delQueue = mutable.Queue[String]()
  val putQueue = mutable.Queue[(String, A)]()

  val map = mutable.Map[String, A]()

  def update(name: String, obj: A) {
    map.update(name, obj)
  }

  def put(name: String, obj: A) {
    putQueue.enqueue((name, obj))
    map.update(name, obj)
  }
  def get(name: String): Option[A] = {
    map.get(name)
  }
  def delete(name: String) {
    delQueue.enqueue(name)
    map -= name
  }
}