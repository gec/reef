/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.client.rest.impl

import net.agileautomata.executor4s._

/**
 * implements the Executor interface by passing all calls through to real executor
 */
trait ExecutorDelegate extends Executor {
  protected def executor: Executor

  final override def operationTimeout = executor.operationTimeout
  final override def execute(fun: => Unit): Unit = executor.execute(fun)
  final override def attempt[A](fun: => A): Future[Result[A]] = executor.attempt(fun)
  final override def schedule(interval: TimeInterval)(fun: => Unit): Timer = executor.schedule(interval)(fun)
  final override def scheduleWithFixedOffset(initial: TimeInterval, offset: TimeInterval)(fun: => Unit): Timer =
    executor.scheduleWithFixedOffset(initial, offset)(fun)
}