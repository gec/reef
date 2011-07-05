
package org.totalgrid.reef.proto

// TODO: get rid of routing keys
object RoutingKeys {

  def measurement(m: Measurements.Measurement): String = m.getName

  def processStatus(m: ProcessStatus.StatusSnapshot): String = m.getInstanceName

  def event(m: Events.Event): String = m.getSubsystem
  def log(m: Events.Log): String = m.getSubsystem

}
