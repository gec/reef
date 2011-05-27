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
package org.totalgrid.reef.shell.proto

case class RequestFailure(why: String) extends Exception(why)

object RequestFailure {
  def interpretAs[R](why: String)(f: => R): R = {
    try f catch { case _ => throw RequestFailure(why) }
  }

  def interpretNilAs[A](why: String)(f: => List[A]): List[A] = {
    val result = f
    if (result.isEmpty) throw RequestFailure(why)
    result
  }
}