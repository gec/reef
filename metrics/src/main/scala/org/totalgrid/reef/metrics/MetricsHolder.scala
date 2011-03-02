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
package org.totalgrid.reef.metrics

/**
 * trait is the main contract between the collectors of the metrics data and the
 * consumers of that data. Metrics can be reset and collected using inclusion filters.
 * The keys are the same format as AMQP routing keys and use the same matching rules.
 * Keys are tokenized by periods, "#" matches everything to end, "*" matches one dot
 * section.
 */
trait MetricsHolder {
  /**
   * clears all matching metrics values back to 0, resets all if no inclusion keys
   * are given
   */
  def reset(keys: List[String] = Nil)

  /**
   * gets a map of all metrics by name, value can be int, double, histogram
   */
  def values(keys: List[String] = Nil): Map[String, Any]
}

/**
 * helper trait that manages a higherlevel map of MetricsHolders
 */
trait MappedMetricsHolder[A <: MetricsHolder] extends MetricsHolder {

  def newHolder(name: String): A

  var stores = Map.empty[String, A]

  def getStore(name: String): A = {
    stores.get(name) match {
      case Some(cv) => cv
      case None =>
        val cmvh = newHolder(name)
        stores = stores + (name -> cmvh)
        cmvh
    }
  }

  def values(keys: List[String]): Map[String, Any] = {
    MetricsMapHelpers.mergeMap(stores.map { case (name, holder) => holder.values(keys) })
  }

  def reset(keys: List[String]) {
    stores.foreach { case (name, holder) => holder.reset(keys) }
  }

}

object MetricsMapHelpers {

  /**
   * calculates rate of measurements per second for all int and double fields.
   */
  def changePerSecond(startValues: Map[String, Any], endValues: Map[String, Any], time: Long): Map[String, Any] = {
    startValues.map {
      case (name, startVal) =>
        endValues.get(name) match {
          case Some(endVal) =>
            endVal match {
              case i: Int => Some(name -> ((i - startVal.asInstanceOf[Int]) * 1000) / time)
              case d: Double => Some(name -> ((d - startVal.asInstanceOf[Double]) * 1000) / time)
              case _ => None
            }
          case None => None
        }
    }.flatten.toMap
  }

  def sumAndCount(metrics: Map[String, Any], key: String): Map[String, Any] = {
    val valsAsDoubles = matchingKeys(metrics, key).map(metrics.get(_).get match {
      case i: Int => i.toDouble
      case d: Double => d
      case _ => throw new Exception("Can not do operations on non numeric metrics.")
    })

    Map(key + ".Sum" -> valsAsDoubles.sum, key + ".Count" -> valsAsDoubles.size)
  }

  def matchingKeys(metrics: Map[String, Any], key: String) = metrics.keys.filter(matches(_, key))

  def mergeMap[A, B](maps: Iterable[Map[A, B]]): Map[A, B] = {
    maps.map { m => m.map { e => e._1 -> e._2 } }.flatten.toMap
  }

  /**
   * checks a name against inclusions list
   * TODO: add exclusion lists
   */
  def matchesAny(metricName: String, keys: List[String]): Boolean = {
    if (keys.isEmpty) return true

    keys.find(matches(metricName, _)).isDefined
  }

  /**
   * matching function based on AMQP routing keys
   * TODO: would java glob matching be better (# -> **)?
   */
  private def matches(routingKey: String, bindingKey: String): Boolean = {
    val r = routingKey.split('.')
    val b = bindingKey.split('.')

    // if there isn't a multi section matcher, the keys need to be the same length to match
    if (!bindingKey.contains("#") && r.size != b.size) return false

    //once we find a section with a '#' the rest of the key doesn't matter
    val removed_hashes = b.takeWhile(!_.contains("#"))
    val nh = removed_hashes.zip(r)
    nh.forall(tuple => tuple._2.matches(tuple._1.replaceAll("\\*", ".*")))
  }
}