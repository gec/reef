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
package org.totalgrid.reef.services.framework

import scala.collection.immutable

/**
 * Simple interface for objects that have a flush/clear transaction behavior.
 */
trait BufferLike {

  /**
   * Flush buffer
   */
  def flush

  /**
   * Clear buffer
   */
  def clear
}

/**
 * Extends BufferNotifications to allow for linking other buffers to the root flush/clear behavior.
 *
 */
trait LinkedBufferLike extends BufferLike {
  private var links = immutable.List.empty[BufferLike]

  /**
   * Link buffers to be flush/cleared together
   */
  def link[A <: BufferLike](obj: A): A = { links ::= obj; obj }

  final override def flush = {
    onFlush
    links.foreach(_.flush)
  }
  final override def clear = {
    onClear
    links.foreach(_.clear)
  }

  protected def onFlush
  protected def onClear
}

