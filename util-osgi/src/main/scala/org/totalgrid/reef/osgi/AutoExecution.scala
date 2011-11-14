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
package org.totalgrid.reef.osgi

import java.util.concurrent.{ ThreadFactory, Executors => JExecutors }
import org.osgi.framework.{ BundleContext, BundleActivator }
import net.agileautomata.executor4s.{ Executor, Executors }

trait ExecutorBundleActivator extends BundleActivator {

  // TODO: rename all activator classes to have more descriptive name (not just Activator)
  private val simpleNameOfClass = this.getClass.getSimpleName

  def threadFactory(subName: String) = new ThreadFactory {
    var threadsCreated = 0
    def newThread(r: Runnable) = this.synchronized {
      threadsCreated += 1
      new Thread(r, simpleNameOfClass + "-" + subName + "-" + threadsCreated)
    }
  }

  private val executor = JExecutors.newCachedThreadPool(threadFactory("pool"))
  private val scheduler = JExecutors.newSingleThreadScheduledExecutor(threadFactory("sched"))
  private val exe = Executors.newCustomExecutor(executor, scheduler)

  final override def start(context: BundleContext) = start(context, exe)

  final override def stop(context: BundleContext) = {
    try {
      stop(context, exe)
    } finally {
      exe.terminate()
    }
  }

  protected def start(context: BundleContext, executor: Executor): Unit
  protected def stop(context: BundleContext, executor: Executor): Unit

}