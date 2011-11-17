package org.totalgrid.reef.process

import java.util.UUID
import com.weiglewilczek.slf4s.Logging
import net.agileautomata.executor4s._

/**
 * Handles the creation of processes and their relationships to each other
 */
class ProcessCoordinator extends Logging {

  type ProcessStartup = ProcessEvents => ProcessBinding

  private case class ProcessNode(
    uuid: UUID,
    events: ProcessEvents,
    startup: ProcessStartup,
    strand: Strand,
    binding: Option[ProcessBinding],
    children: Set[UUID],
    retry: TimeInterval,
    timer: Option[Timer])

  private case class State(processes: Map[UUID, ProcessNode]) {

    def create(startup: ProcessStartup, node: ProcessNode) : State = copy(processes = processes + (node.uuid -> node))

    def swap(uuid: UUID)(fun: ProcessNode => ProcessNode): State = {
      processes.get(uuid) match {
        case Some(node) =>
          this.copy(processes = processes + (uuid -> fun(node)))
        case None =>
          logger.error("Process uuid not found: " + uuid)
          this
      }
    }

    def start(uuid: UUID): State = swap(uuid)(n0 => n0.copy(timer = None, binding = Some(n0.startup(n0.events))))

    def startTimer(uuid: UUID)(f: => Unit): State = swap(uuid)(n0 => n0.copy(timer = Some(n0.strand.schedule(n0.retry)(f))))

  }

  private val mutex = new Object
  private var state = State(Map.empty[UUID, ProcessNode])

  private def mutate(fun: State => State) = mutex.synchronized(state = fun(state))

  /* --- Process event handlers */

  private def onBindingFailure(uuid: UUID, msg: String) = {

  }

  private def onBindingStop(uuid: UUID) = {

  }

  private  def onBindingException(uuid: UUID, ex: Exception) = mutate { s0 =>
    logger.error("Process failed with unhandled exception", ex)
    s0.startTimer(uuid)(onBindingRestart(uuid))
  }

  private def onBindingRestart(uuid: UUID) = mutate { s0 =>
    s0.s
  }

  /* --- Creation functions --- */

  /**
   * Create a process that retries on failure after a time interval
   */
  def retry(strand: Strand, interval: TimeInterval)(startup: ProcessStartup): Unit = {
    val uuid = java.util.UUID.randomUUID
    val node = ProcessNode(uuid, getProcessEvents(uuid), startup, strand, None, Set.empty[UUID], interval, None)
    mutate(_.create(startup, node))
    val handler = new ExceptionHandler {
      def onException(ex: Exception) = onBindingException(uuid, ex)
    }
    strand.addExceptionHandler(handler) // if an unhandled exception occurs on the strand, notify
    strand.execute(mutate(_.start(uuid))) // start the process on it's strand
  }

  /*--- private helpers --- */

  def getProcessEvents(uuid: UUID) = new ProcessEvents {
    def onFailure(msg: String) = onBindingFailure(uuid, msg)
    def onStop() = onBindingStop(uuid)
  }


}

