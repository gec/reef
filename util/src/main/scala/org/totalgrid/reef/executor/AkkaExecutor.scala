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

package org.totalgrid.reef.executor

import parallel.Future
import akka.actor.ActorRef
import akka.actor.Actor._
import org.totalgrid.reef.util.Timer
import org.totalgrid.reef.executor.ActorExecutorMessages.{ Request, Execute }

/**
 *  Generic actor that can be used to execute
 *  arbitrary blocks of code. Useful for synchronizing
 *  lots of other threads.
 *
 */
class AkkaExecutor extends Executor with Lifecycle {

  private var currentActor: Option[ActorRef] = None

  final override def doStart() = currentActor = Some(newExecutorActor)

  private val timers = scala.collection.mutable.Set.empty[Timer]

  private def addTimer(timer: Timer): Timer = mutex.synchronized {
    timers.add(timer)
    timer
  }

  private def removeTimer(timer: Timer) = mutex.synchronized(timers.remove(timer))

  final override def doStop() = {
    currentActor.foreach(_.stop())
    currentActor = None
    timers.foreach(_.cancel())
  }

  def newExecutorActor: ActorRef = actorOf[AkkaExecutorActor].start()

  private def withActorRef(fun: ActorRef => Unit) = currentActor match {
    case Some(a) => fun(a)
    case None => throw new IllegalStateException("Executor is not running")
  }

  /* --- Implement Executor --- */

  /// sends a unit of work to the actor
  def execute(fun: => Unit) = {
    withActorRef(_ ! Execute(() => fun))
  }

  /// sends a unit of work to do after a specficied time has elapsed.    
  def delay(msec: Long)(fun: => Unit): Timer = addTimer(new SingleAkkaTimer(msec, this)(removeTimer)(fun))

  /// send a unit of work to do immediatley and then repeat function until the actor is killed
  def repeat(msec: Long)(fun: => Unit): Timer = addTimer(new RepeatAkkaTimer(msec, this)(removeTimer)(fun))

  /// execute a function synchronously and return the value
  def request[A](fun: => A): Future[A] = {
    val future = new SynchronizedFuture[A]()
    withActorRef(_ ! Request(() => fun, future.set))
    future
  }

}

