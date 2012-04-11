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

object ArgumentUtilites {
  def calculateOverloadScore(c: PreparableApiCall[_]) = {
    val overloadScorer = new OverloadSignatureScorer

    c.prepareFunction(overloadScorer)

    overloadScorer.score
  }

  def argumentsAsString(c: PreparableApiCall[_]) = {
    val collector = new ArgumentCollector

    c.prepareFunction(collector)

    collector.arguments
  }
}

/**
 * when we are calling the prepareFunction of an api call to collect information
 * on the call itself (a cheap type of reflection) we don't care what value we
 * return so we return a null. This is ok because it will be used.
 */
abstract class UtilityArgumentSource extends ArgumentSource {

  def handleArgument[A](name: String, klass: Class[A])

  def findArgument[A](name: String, klass: Class[A]) = {
    handleArgument(name, klass)
    Some(null.asInstanceOf[A])
  }

  def findArguments[A](name: String, klass: Class[A]) = {
    findArgument(name, klass).toList
  }
}

class OverloadSignatureScorer extends UtilityArgumentSource {

  var score = 0L

  def handleArgument[A](name: String, klass: Class[A]) {
    score *= 10
    klass match {
      // sorted in order of increasing specificity
      case StringClass => score += 1
      case DoubleClass => score += 2
      case IntClass => score += 3
      case LongClass => score += 4
      case BooleanClass => score += 5
      case ReefUuidClass => score += 6
      case ReefIdClass => score += 7
      case _ => score += 8
    }
  }
}

class ArgumentCollector extends UtilityArgumentSource {

  private var argumentSignatures = List.empty[String]

  def arguments = "(" + argumentSignatures.reverse.mkString(", ") + ")"

  def handleArgument[A](name: String, klass: Class[A]) {
    argumentSignatures ::= name + ": " + klass.getSimpleName
  }
}
