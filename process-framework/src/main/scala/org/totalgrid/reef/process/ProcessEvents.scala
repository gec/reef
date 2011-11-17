package org.totalgrid.reef.process

/**
 * A running sub-application calls there handlers to notify the process framework when things happen
 */
trait ProcessEvents {

  /**
   * Report that a process has cleanly stopped
   */
  def onStop()

  /**
   * Report that a process has failed
   */
  def onFailure(msg: String)
}