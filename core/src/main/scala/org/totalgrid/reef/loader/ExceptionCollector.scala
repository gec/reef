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
package org.totalgrid.reef.loader

class LoadingException(msg: String) extends RuntimeException(msg)

trait ExceptionCollector {
  def collect[A](name: => String)(f: => Unit)
}

class LoadingExceptionCollector extends ExceptionCollector {

  private var errors = List.empty[(String, Exception)]

  private var firstError = false

  def collect[A](name: => String)(f: => Unit) {
    try {
      f
    } catch {
      case ex: LoadingException =>
        if (!firstError) {
          println("Errors detected, attempting to continue loading, not all errors may be detected until these errors are fixed.")
        }
        firstError = true
        val nameString = try { name } catch { case _ => "<unknown>" }
        errors ::= (nameString + " - " + ex.getMessage, ex)
    }
  }

  def getErrors: List[String] = errors.map { _._1 }
}