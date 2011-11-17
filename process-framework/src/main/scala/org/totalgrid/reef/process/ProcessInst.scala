package org.totalgrid.reef.process

import net.agileautomata.executor4s.{Executor}
import java.lang.Object


//case class ProcessBinding(exe: Executor)(onStart: => Unit)(onStop: => Unit)

/**
 * A process is an asynchronous chain of events that occurs on an executor
 * When it fails, it must recover
 */
trait ProcessInst {

  sealed trait State
  case object Idle extends State
  case object Running extends State
  case object Stopping extends State
  case object Stopped extends State
  case object Failed extends State

  private val mutex = new Object
  private var listeners = Set.empty[State => Unit]
  private var state : State = Idle

  /**
   * Listen to a process's state
   */
  def listen(onStateChange: State => Unit): Unit = mutex.synchronized(listeners += onStateChange)

  /**
   * sends a stop signal to the process. Does not block. Listener's will wait for a
   * stopped event.
   */
  def start() : Unit

  /**
   * sends a stop signal to the process. Does not block. Listener's will wait for a
   * stopped event.
   */
  def stop() : Unit

  /**
   *
   */
  def changeState(s: State) = mutex.synchronized {
    state = s
    listeners.foreach(_.apply(s))
  }




}