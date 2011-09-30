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

package org.totalgrid.reef.loader

import org.totalgrid.reef.util.Logging

class LoadingException(msg: String) extends RuntimeException(msg)

/**
 * we try to segment the loading of config file into as sub sections as possible so we catch
 * and record all LoadExceptions and continue trying to parse the file so the integrator gets
 * as many errors as possible on each pass
 */
trait ExceptionCollector {
  /**
   * loading code should call use a collect blocks around each separable unit of loading.
   * names generally end with a colon and space
   */
  def collect[A](name: => String)(function: => Unit)

  def hasErrors: Boolean

  def getErrors: List[String]

  def getErrorTuples: List[(String, Exception)]

  def reset()
}

class LoadingExceptionCollector extends ExceptionCollector with Logging {

  private var errors = List.empty[(String, Exception)]

  private var firstError = false

  def collect[A](name: => String)(function: => Unit) {
    try {
      function
    } catch {
      case ex: LoadingException =>
        if (!firstError) {
          println("Errors detected, attempting to continue loading, not all errors may be detected until these errors are fixed.")
        }
        firstError = true
        val nameString = try {
          name
        } catch {
          case _ => "<unknown>"
        }
        logger.info("exception encountered during loading: " + nameString + ": " + ex.getMessage)
        //        logger.debug("DEBUG: exception encountered during loading: " + nameString + ": " + ex.getMessage + ": ", ex)
        addError(nameString, ex)
    }
  }

  def getErrors: List[String] = errors.map {
    _._1
  }

  def getErrorTuples: List[(String, Exception)] = {
    errors.toList
  }

  def hasErrors: Boolean = firstError

  def addError(name: String, ex: Exception) {
    errors = errors ::: ((name + " - " + ex.getMessage, ex) :: Nil)
  }

  def reset() {
    errors = List.empty[(String, Exception)]
    firstError = false
  }
}