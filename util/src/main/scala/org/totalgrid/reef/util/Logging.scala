/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.util

import org.slf4j.{ Logger, LoggerFactory }

/**
 * Provides a scala-fied interface to slf4j. widens the logger interface to include
 * the use of lazy logging via closures.
 */
trait Logging {
  // named so we dont conflict with the field "log" in standard java classes that use slf4j
  private val reefLogger = LoggerFactory.getLogger(getClass)

  protected def trace(f: => String): Unit = if (reefLogger.isTraceEnabled) reefLogger.trace(f)
  protected def debug(f: => String): Unit = if (reefLogger.isDebugEnabled) reefLogger.debug(f)
  protected def info(f: => String): Unit = if (reefLogger.isInfoEnabled) reefLogger.info(f)
  protected def warn(f: => String): Unit = if (reefLogger.isWarnEnabled) reefLogger.warn(f)
  protected def error(f: => String): Unit = if (reefLogger.isErrorEnabled) reefLogger.error(f)

  protected def trace(ex: Throwable): Unit = trace { ex.toString }
  protected def debug(ex: Throwable): Unit = debug { ex.toString }
  protected def info(ex: Throwable): Unit = info { ex.toString }
  protected def warn(ex: Throwable): Unit = warn { ex.toString }
  protected def error(ex: Throwable): Unit = error { ex.toString }

  protected def trace(msg: String, ex: Throwable): Unit = reefLogger.trace(msg, ex)
  protected def debug(msg: String, ex: Throwable): Unit = reefLogger.debug(msg, ex)
  protected def info(msg: String, ex: Throwable): Unit = reefLogger.info(msg, ex)
  protected def warn(msg: String, ex: Throwable): Unit = reefLogger.warn(msg, ex)
  protected def error(msg: String, ex: Throwable): Unit = reefLogger.error(msg, ex)

}
