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
package org.totalgrid.reef.app.process

import com.typesafe.scalalogging.slf4j.Logging

import net.agileautomata.executor4s._
import scala.collection.immutable.Queue
import scala.util.Random

/**
 * this is a wrapper used by the ProcessManager to manage the lifecycle of the user code in
 * Process. User code should not use this class.
 */
class TaskState(val payloadLogic: Process, p: ProcessManager, exe: Executor) extends Logging {
  var children = Queue.empty[TaskState]
  var parent = Option.empty[TaskState]
  var setupFuture = Option.empty[Future[_]]

  var isActive = false
  var isStopping = true
  var isFailed = false
  var retryTimer = Option.empty[Timer]
  var nextRetryDelay = payloadLogic.setupRetryDelay

  def shouldBeStarted = this.synchronized {
    val parentRunning = parent.map { _.isActive }.getOrElse(true)

    parentRunning && !isActive && retryTimer == None
  }

  def shouldBeStopped = this.synchronized {
    def parentStopping = parent.map { _.isStopping }.getOrElse(false)

    def childrenFailed = children.find { _.isFailed }

    (!isStopping || retryTimer.isDefined || isActive) &&
      (parentStopping || (payloadLogic.failIfChildFails && childrenFailed.isDefined))
  }

  def name = payloadLogic.name

  def start() {
    val future = this.synchronized {
      logger.debug(name + " starting")
      isStopping = false
      isFailed = false
      val f = exe.attempt(payloadLogic.setup(p))
      setupFuture = Some(f)
      f
    }

    future.listen { r =>
      this.synchronized {
        retryTimer = None
        setupFuture = None
        r match {
          case Success(_) =>
            nextRetryDelay = payloadLogic.setupRetryDelay
            if (!isStopping) {
              logger.debug(name + " started")
              isActive = true
              startChildren()
            } else {
              logger.debug(name + " started but stop requested, stopping")
              cleanupProcess()
            }
          case Failure(ex) =>
            p.reportError(payloadLogic, "Failure during " + name, Some(ex))
            logger.info(name + " failed setup. " + ex.getMessage, ex)
            if (payloadLogic.setupExceptionIsFailure) {
              fail()
            } else {
              scheduleRetrySetup(false)
            }
        }
        notify()
      }

    }
  }

  def stop() = {
    this.synchronized {
      logger.debug(name + " stopping")
      isStopping = true
      if (setupFuture.isDefined) wait()
    }

    retryTimer.foreach { t =>
      logger.debug(name + " canceling retry")
      t.cancel()
    }

    this.synchronized {
      stopChildren()
      if (isActive) cleanupProcess()
      isActive = false
    }
  }

  private def cleanupProcess() {
    try {
      payloadLogic.cleanup(p)
    } catch {
      case ex: Exception =>
        logger.warn("Exception during process " + name + " cleanup: " + ex.getMessage, ex)
        p.reportError(payloadLogic, "Failure during " + name + " cleanup", Some(ex))
    }
  }

  private def startChildren() {
    val toStart = children.filter { _.shouldBeStarted }

    toStart.foreach { child =>
      logger.debug(name + " starting child: " + child.name.trim())
      child.start()
    }
  }

  private def stopChildren() {
    val toStop = children.filter { _.shouldBeStopped }

    toStop.foreach { child =>
      logger.debug(name + " stoppping child: " + child.name.trim())
      child.stop()
    }
  }

  private def failParent() {

    parent.filter { _.shouldBeStopped }.foreach { p =>
      logger.debug(name + " failing parent: " + p.name.trim())
      p.fail()
    }
  }

  def fail() = this.synchronized {
    isFailed = true
    logger.info(name + " failed!")
    stop()
    stopChildren()
    failParent()
    if (payloadLogic.retryAfterFailure) {
      scheduleRetrySetup(true)
    }
  }

  private def scheduleRetrySetup(isFailure: Boolean) {
    val extraMessage = if (isFailure) " after failure" else ""
    val delay = (Random.nextInt(nextRetryDelay.toInt) + 1).milliseconds
    nextRetryDelay = (nextRetryDelay * 2).min(payloadLogic.setupRetryDelayMax)
    retryTimer = Some(exe.schedule(delay) {
      logger.debug(name + " retrying setup" + extraMessage)
      start()
    })
    logger.info(name + " retrying setup" + extraMessage + " in: " + delay)
  }

  def setParent(p: TaskState) = this.synchronized {
    if (parent.isDefined) throw new IllegalArgumentException(name + " already has parent " + parent.get.name)
    parent = Some(p)
  }

  def addChild(child: TaskState) = this.synchronized {
    child.setParent(this)
    children = children.enqueue(child)
    if (isActive) startChildren()
  }

  def removeChild(child: TaskState) = this.synchronized {
    val c = children.find(_ == child)
    if (c.isEmpty) throw new IllegalArgumentException(child.name + " not child of " + name)
    children = children.filterNot(_ == child)

    if (children.find(_ == child).isDefined) throw new IllegalArgumentException(child.name + " still child of " + name)
  }
}