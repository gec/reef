/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Measurements
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.sapi.client.ClientSession
import org.totalgrid.reef.sapi.{ Destination, AddressableDestination }
import org.totalgrid.reef.models.Point

/**
 * interface for publishing the current values of summary points. When there are many processes all trying
 * to update the same summary values it is easiest conceptually to split apart the inital value calcualtion
 * and the incrementing of the value. We plan on moving the summary point aggragation to a dedicated tool
 * like redis which has built in increment operations and using this interface should make that transition
 * easy.
 */
trait SummaryPoints {

  /**
   * set the current value of the point, should only be done once at beginning of counting (for now thats
   * on startup of the calculator)
   */
  def setSummary(name: String, value: Int)

  /**
   * called every time the value goes up or down, calling with incr == 0 returns instantly
   */
  def incrementSummary(name: String, incr: Int)
}

/**
 * stores the current value of the summary points for inspect via testing. If using multi-threaded tests
 * an onPublish callback can be provided to be notified of updates.
 */
class SilentSummaryPoints(onPublish: (String, Int) => Any = (x, y) => {}) extends SummaryPointHolder {
  override def publish(name: String, current: ValueHolder) {
    onPublish(name, current.increments + current.initialValue)
  }

  /**
   * Get the current value of a point (if it exists)
   */
  def getValue(name: String): Option[Int] = currentValues.get(name).map(c => c.increments + c.initialValue)

  /**
   * Get all of the current values as a map of name -> value
   */
  def getMap(): Map[String, Int] = currentValues.map { case (n, c) => n -> (c.increments + c.initialValue) }
}

/**
 * base trait for summary point publishes that holds the initial value and # of increments for each point
 */
trait SummaryPointHolder extends SummaryPoints with Logging {
  protected case class ValueHolder(var increments: Int, var initialValue: Int, var initialSet: Boolean) {
    override def toString() = {
      (initialValue + increments) + (if (initialSet) " (" + initialValue + ")" else " (-)")
    }
  }
  protected var currentValues = Map.empty[String, ValueHolder]

  /**
   * sub classes need to implement this function to publish values to the correct data sinks
   */
  protected def publish(name: String, current: ValueHolder)

  def incrementSummary(name: String, incr: Int) {
    if (incr == 0) return
    currentValues.synchronized {
      currentValues.get(name) match {
        case Some(current) =>
          current.increments += incr
          publish(name, current)
        case None =>
          val newVal = ValueHolder(incr, 0, false)
          currentValues = currentValues + (name -> newVal)
          publish(name, newVal)
      }
    }
  }

  def setSummary(name: String, value: Int) {
    currentValues.synchronized {
      val currentVal = currentValues.get(name) match {
        case Some(old) =>
          val newSet = ValueHolder(old.increments, value, true)
          warn { "overwriting currently set summary value: " + old + " with: " + newSet }
          currentValues = (currentValues - name) + (name -> newSet)
          newSet
        case None =>
          val startingValue = ValueHolder(0, value, true)
          currentValues = currentValues + (name -> startingValue)
          startingValue
      }
      publish(name, currentVal)
    }
  }

}

/**
 * first pass SummaryPoints provider, pushes values to AMQP when the meas_proc is ready
 */
class SummaryPointPublisher(amqp: AMQPProtoFactory) extends SummaryPointHolder with Logging {

  private type Channel = Measurements.MeasurementBatch => Unit

  private var outputChannels = Map.empty[String, Channel]
  private var clients = Map.empty[String, Channel]

  /**
   * attempt to publish the current value iff the point is enabled and a meas_proc is online
   * @param name
   * @param current
   */
  override def publish(name: String, current: ValueHolder) {
    val channel = outputChannels.get(name) match {
      case Some(channel) =>
        publishValue(name, current, channel)
      case None =>
        findChannel(name) match {
          case Some(channel) =>
            outputChannels = outputChannels + (name -> channel)
            publishValue(name, current, channel)
          case None =>
        }
    }
  }

  private def publishValue(name: String, value: ValueHolder, outputChannel: Channel) {

    if (!value.initialSet) return ;

    val batch = Measurements.MeasurementBatch.newBuilder.setWallTime(System.currentTimeMillis)
    val point = Measurements.Measurement.newBuilder.setName(name)
      .setQuality(Measurements.Quality.newBuilder.build)
      .setUnit("raw")
      .setIntVal(value.increments + value.initialValue)
      .setType(Measurements.Measurement.Type.INT)
      .setTime(System.currentTimeMillis)
    batch.addMeas(point)

    outputChannel(batch.build)
  }

  /**
   * tries to go from the name of the summary point to the output measurement exchange name, a meas_proc must
   * be running for us to find a channel.
   * TODO: handle case where meas_proc is lost
   * @param name
   * @return
   */
  private def findChannel(name: String): Option[Channel] = {

    //TODO reenable summary points

    import org.squeryl.PrimitiveTypeMode._
    import org.totalgrid.reef.models.ApplicationSchema
    var ret: Option[Channel] = None
    transaction {
      Point.findByName(name).headOption match {
        case Some(point) =>
          point.endpoint.value match {
            case Some(ce) =>
              ce.frontEndAssignment.value.serviceRoutingKey match {
                case Some(routingKey) =>

                  warn { "SummaryPoint: " + name + " publishing to: " + routingKey }

                  // all publishing to the same meas proc uses the same client which can then throttle failures
                  clients.get(routingKey) match {
                    case Some(callback) => ret = Some(callback)
                    case None =>
                      val client = amqp.getProtoClientSession(ReefServicesList, 1000)
                      val func = publishMeasurement(client, LastAttempt(0, true), AddressableDestination(routingKey)) _
                      clients += (routingKey -> func)
                      ret = Some(func)
                  }

                case None => warn { "SummaryPoint: " + name + " has no meas proc." }
              }
            case None => warn { "SummaryPoint: " + name + " has no endpoint" }
          }
        case None => debug { "SummaryPoint: " + name + " not configured" }
      }
    }
    ret

  }

  /**
   * helper class that allows us to retry each client on a seperate interval
   */
  case class LastAttempt(var nextTime: Long, var success: Boolean)

  // TODO - refactor this code to be more functional

  private def publishMeasurement(client: ClientSession, lastAttempt: LastAttempt, dest: Destination)(mb: Measurements.MeasurementBatch) {
    val now = System.currentTimeMillis
    if (!lastAttempt.success && now < lastAttempt.nextTime) {
      info { "failed last time, skipping publishing summary until: " + lastAttempt.nextTime }
      return
    }
    try {
      lastAttempt.nextTime = now + 5000
      client.put(mb, destination = dest).await().expectMany()
      lastAttempt.success = true
    } catch {
      case e: Exception =>
        lastAttempt.success = false
        error { e }
    }
  }
}