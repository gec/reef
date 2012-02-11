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
package org.totalgrid.reef.httpbridge.servlets.helpers

import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }
import org.totalgrid.reef.client.exception.BadRequestException
import com.google.protobuf.{ ByteString, Message }

/**
 * An argument source gets the "typed" class for named paramters. These arguments
 * may be collected from URL, headers, posted data of various formats.
 *
 * Implementing classes only need to define findArgument, findArguments. The rest
 * of the calls are provided by ArgumentSourceHelpers
 */
trait ArgumentSource extends ArgumentSourceHelpers {

  def findArgument[A](name: String, klass: Class[A]): Option[A]
  def findArguments[A](name: String, klass: Class[A]): List[A]

}

/**
 * "Fat-Trait" for defining lots of helpers around findArgument(s) functions
 */
trait ArgumentSourceHelpers { self: ArgumentSource =>

  /**
   * classes we expect most ArgumentSources be capable of implementing
   *
   * Note: we use the java.lang.* objects rather than scala objects here because
   * otherwise the class.cast() function null pointers on those values.
   */
  val StringClass = classOf[String]
  val IntClass = classOf[java.lang.Integer]
  val MessageClass = classOf[Message]
  val LongClass = classOf[java.lang.Long]
  val DoubleClass = classOf[java.lang.Double]
  val ReefUuidClass = classOf[ReefUUID]
  val ReefIdClass = classOf[ReefID]
  val BooleanClass = classOf[java.lang.Boolean]
  //val ByteStringClass = classOf[ByteString]

  // TODO: enable typed protobuf argument parsing

  def getInt(name: String) = require(name, findArgument(name, IntClass))
  def getLong(name: String) = require(name, findArgument(name, LongClass))
  def getBoolean(name: String) = require(name, findArgument(name, BooleanClass))
  def getDouble(name: String) = require(name, findArgument(name, DoubleClass))
  def getString(name: String) = require(name, findArgument(name, StringClass))
  def getUuid(name: String) = require(name, findArgument(name, ReefUuidClass))
  def getId(name: String) = require(name, findArgument(name, ReefIdClass))
  //def getByteArray(name: String) = require(name, findArgument(name, ByteStringClass)).toByteArray

  def getInts(name: String) = require(name, findArguments(name, IntClass))
  def getLongs(name: String) = require(name, findArguments(name, LongClass))
  def getBooleans(name: String) = require(name, findArguments(name, BooleanClass))
  def getDoubles(name: String) = require(name, findArguments(name, DoubleClass))
  def getStrings(name: String) = require(name, findArguments(name, StringClass))
  def getUuids(name: String) = require(name, findArguments(name, ReefUuidClass))

  def require[A](name: String, result: Option[A]): A = {
    result.getOrElse(throw new BadRequestException("Missing required argument: " + name))
  }
  def require[A](name: String, result: List[A]): List[A] = if (result.isEmpty) {
    throw new BadRequestException("Missing required list argument: " + name)
  } else result

  def getArgument[A](name: String, klass: Class[A]): A = require(name, findArgument(name, klass))
  def getArguments[A](name: String, klass: Class[A]): List[A] = require(name, findArguments(name, klass))

}