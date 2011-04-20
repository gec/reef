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
package org.totalgrid.reef.shell.admin

import org.apache.felix.service.command.CommandSession

/**
 * holds an objects in the command session to maintain state between command
 * invocations
 */
class SessionHeldObject[A](name: String, session: => CommandSession, default: A) {
  def get(): A = {
    session.get(name) match {
      case null => default
      case o: Object => o.asInstanceOf[A]
    }
  }
  def set(obj: A) = session.put(name, obj)
  def clear() = session.put(name, null)
}

/**
 * wrapper around the object holder that adds some list manipulation functions
 */
class SessionHeldList[A](name: String, session: => CommandSession) extends SessionHeldObject[List[A]](name, session, Nil: List[A]) {

  def add(key: A) = set(key :: get)
  def remove(key: A) = set(get.filterNot(_ == key))
}

