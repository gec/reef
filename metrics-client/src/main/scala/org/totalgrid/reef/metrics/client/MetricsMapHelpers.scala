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
package org.totalgrid.reef.metrics.client

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
              case i: Int => Some(name + ".Rate" -> (((i - startVal.asInstanceOf[Int]) * 1000) / time).toInt)
              case d: Double => Some(name + ".Rate" -> ((d - startVal.asInstanceOf[Double]) * 1000) / time)
              case _ => None
            }
          case None => None
        }
    }.flatten.toMap
  }

  def performCalculations(values: Map[String, Any], calculations: List[String]): Map[String, Any] = {
    val calcedValues = calculations.map { MetricsMapHelpers.sumAndCount(values, _) }
    MetricsMapHelpers.mergeMap(values :: calcedValues)
  }

  def sumAndCount(metrics: Map[String, Any], key: String): Map[String, Any] = {
    val sourceData = matchingKeys(metrics, key)
    if (sourceData.isEmpty) return Map.empty[String, Any]

    val valsAsDoubles = sourceData.map(metricName => metrics.get(metricName).get match {
      case i: Int => i.toDouble
      case d: Double => d
      case x: Any => throw new Exception("Can not do operations on non numeric metrics : " + x.asInstanceOf[AnyRef].getClass + " , " + metricName)
    })

    Map(
      key + ".Sum" -> valsAsDoubles.sum,
      key + ".Count" -> valsAsDoubles.size)
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