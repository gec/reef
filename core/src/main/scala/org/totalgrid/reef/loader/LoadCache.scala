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
package org.totalgrid.reef.loader

import scala.collection.mutable.HashMap
import org.totalgrid.reef.util.Logging

/**
 * When loading a configuration, we need to validate the equipment model
 * against the communications model. This class records the points and
 * controls found, then does a validation at the end.
 */
class LoadCache extends Logging {

  val controls = HashMap[String, CachedControl]()
  val points = HashMap[String, CachedPoint]()

  val loadCacheEqu = new LoadCacheEqu(this)
  val loadCacheCom = new LoadCacheCom(this)

  def validate = {
    import ValidateResult._

    if (controls.isEmpty && points.isEmpty)
      println("WARNING: No controls or points found.")

    var pEquOnly = List[CachedObject]()
    var pComOnly = List[CachedObject]()
    var pComplete = List[CachedObject]()
    var pMultiple = List[CachedObject]()
    var pNone = List[CachedObject]()

    if (!points.isEmpty) {

      points.values.foreach(point => {
        point.validate match {
          case NONE => pEquOnly ::= point
          case COMPLETE => pComplete ::= point
          case EQU_ONLY => pEquOnly ::= point
          case COM_ONLY => pComOnly ::= point
          case MULTIPLE => pMultiple ::= point
        }
      })
      pEquOnly = pEquOnly.sortBy(_.name)
      pComOnly = pComOnly.sortBy(_.name)
      pMultiple = pMultiple.sortBy(_.name)
      pNone = pNone.sortBy(_.name)

      println("POINTS SUMMARY:  ")
      println("   Points found in equipmentModel and communicationsModel: " + pComplete.length)
      println("   Points found in equipmentModel only:      " + pComOnly.length)
      println("   Points found in communicationsModel only: " + pEquOnly.length)
      if (pMultiple.length > 0)
        println("   Points found in both models more than once: " + pMultiple.length)
      if (pNone.length > 0)
        println("   Points found in neither model (internal error): " + pNone.length)
    }

    var cEquOnly = List[CachedObject]()
    var cComOnly = List[CachedObject]()
    var cComplete = List[CachedObject]()
    var cMultiple = List[CachedObject]()
    var cNone = List[CachedObject]()

    if (!controls.isEmpty) {

      controls.values.foreach(control => {
        control.validate match {
          case NONE => cEquOnly ::= control
          case COMPLETE => cComplete ::= control
          case EQU_ONLY => cEquOnly ::= control
          case COM_ONLY => cComOnly ::= control
          case MULTIPLE => cMultiple ::= control
        }
      })
      cEquOnly = cEquOnly.sortBy(_.name)
      cComOnly = cComOnly.sortBy(_.name)
      cMultiple = cMultiple.sortBy(_.name)
      cNone = cNone.sortBy(_.name)

      println("CONTROLS SUMMARY:  ")
      println("   Controls found in equipmentModel and communicationsModel: " + cComplete.length)
      println("   Controls found in equipmentModel only:      " + cComOnly.length)
      println("   Controls found in communicationsModel only: " + cEquOnly.length)
      if (cMultiple.length > 0)
        println("   Controls found in one model more than once: " + cMultiple.length)
      if (cNone.length > 0)
        println("   Controls found in neither model (internal error): " + cNone.length)

    }
    println

    if (cEquOnly.length > 0 || cComOnly.length > 0 || cMultiple.length > 0 || cNone.length > 0) {
      println("POINT WARNINGS:")
      if (pEquOnly.length > 0)
        println("  Points found in equipmentModel only: \n" + pEquOnly.mkString("    ", "\n    ", "\n"))
      if (pComOnly.length > 0)
        println("  Points found in communicationsModel only:\n" + pComOnly.mkString("    ", "\n    ", "\n"))
      if (pMultiple.length > 0)
        println("  Points found in both models more than once:\n" + pMultiple.mkString("    ", "\n    ", "\n"))
      if (pNone.length > 0)
        println("  Points found in neither model (internal error):\n" + pNone.mkString("    ", "\n    ", "\n"))
    }

    if (cEquOnly.length > 0 || cComOnly.length > 0 || cMultiple.length > 0 || cNone.length > 0) {
      println("CONTROL WARNINGS:")
      if (cEquOnly.length > 0)
        println("  Controls found in equipmentModel only:\n" + cEquOnly.mkString("    ", "\n    ", "\n"))
      if (cComOnly.length > 0)
        println("  Controls found in communicationsModel only:\n" + cComOnly.mkString("    ", "\n    ", "\n"))
      if (cMultiple.length > 0)
        println("  Controls found in both models more than once:\n" + cMultiple.mkString("    ", "\n    ", "\n"))
      if (cNone.length > 0)
        println("  Controls found in neither model (internal error):\n" + cNone.mkString("    ", "\n    ", "\n"))
    }
  }
  println

  val pWarnings = points.values.filterNot(_.warnings.isEmpty)
  if (!pWarnings.isEmpty) {
    pWarnings.foreach(p =>
      println("WARNINGS for Point '" + p.name + "':\n    " + p.warnings.mkString("\n    ")))
    println
  }

  val cWarnings = controls.values.filterNot(_.warnings.isEmpty)
  if (!cWarnings.isEmpty) {
    cWarnings.foreach(c =>
      println("WARNINGS for Control '" + c.name + "':\n    " + c.warnings.mkString("\n    ")))
    println
  }

}

class CacheType(val cache: LoadCache) {

  def reset = {
    cache.controls.clear
    cache.points.clear
  }
}

class LoadCacheEqu(override val cache: LoadCache) extends CacheType(cache) {

  def addPoint(name: String, unit: String) = {
    cache.points.get(name) match {
      case Some(p) => p.addReference(this, "", unit)
      case _ => cache.points += (name -> new CachedPoint(this, name, unit))
    }
  }
  def addControl(name: String) = {
    cache.controls.get(name) match {
      case Some(c) => c.addReference(this)
      case _ => cache.controls += (name -> new CachedControl(this, name))
    }

  }
  //override def getType = "equipmentModel"
}

class LoadCacheCom(override val cache: LoadCache) extends CacheType(cache) {

  def addPoint(endpointName: String, name: String, index: Int, unit: String = "") = {
    cache.points.get(name) match {
      case Some(p) => p.addReference(this, endpointName, unit, index)
      case _ => cache.points += (name -> new CachedPoint(this, endpointName, name, unit, index))
    }

  }
  def addControl(endpointName: String, name: String, index: Int) = {
    cache.controls.get(name) match {
      case Some(c) => c.addReference(this, endpointName, index)
      case _ => cache.controls += (name -> new CachedControl(this, endpointName, name, index))
    }

  }
}

abstract class ValidateResult
object ValidateResult {
  case object NONE extends ValidateResult
  case object COM_ONLY extends ValidateResult
  case object EQU_ONLY extends ValidateResult
  case object COMPLETE extends ValidateResult
  case object MULTIPLE extends ValidateResult // Multiple com or equ references. Is this ever good?
}

class CachedObject(referencedFrom: CacheType, val name: String) {
  var errors = List[String]()
  var warnings = List[String]()
  var comCount = if (referencedFrom.isInstanceOf[LoadCacheCom]) 1 else 0
  var equCount = if (referencedFrom.isInstanceOf[LoadCacheEqu]) 1 else 0

  def incrementReference(referencedFrom: CacheType) = {
    referencedFrom match {
      case c: LoadCacheCom => comCount += 1
      case e: LoadCacheEqu => equCount += 1
    }
  }

  def validate: ValidateResult = {
    import ValidateResult._

    if (comCount == 0 && equCount == 0) {
      NONE
    } else if (comCount == 1 && equCount == 1) {
      // Good
      COMPLETE
    } else if (comCount == 0 && equCount == 1) {
      EQU_ONLY
    } else if (comCount == 1 && equCount == 0) {
      COM_ONLY
    } else {
      MULTIPLE
    }
  }

  override def toString = name
}

class CachedPoint(
    referencedFrom: CacheType,
    var endpointName: String,
    override val name: String,
    var unit: String,
    index: Int) extends CachedObject(referencedFrom, name) {

  def this(referencedFrom: CacheType, _name: String, _unit: String) = this(referencedFrom, "", _name, _unit, -1)
  def this(referencedFrom: CacheType, _endpointName: String, _name: String, _index: Int) = this(referencedFrom, _endpointName, _name, "", _index)

  def addReference(referencedFrom: CacheType, _endpointName: String, _unit: String = "", _index: Int = -1) = {
    if (_endpointName.length > 0) {
      if (endpointName.length > 0)
        warnings ::= "point '" + name + "' is referenced by two endpoints: '" + endpointName + "' and '" + _endpointName + "'"
      endpointName = _endpointName
    }

    if (_unit.length > 0) {
      if (unit.length > 0)
        warnings ::= "point '" + name + "' is referenced by two endpoints: '" + unit + "' and '" + _unit + "'"
      unit = _unit
    }

    incrementReference(referencedFrom)
  }

}

class CachedControl(
    referencedFrom: CacheType,
    var endpointName: String,
    override val name: String,
    var index: Int = -1) extends CachedObject(referencedFrom, name) {

  def this(referencedFrom: CacheType, _name: String) = this(referencedFrom, "", _name)

  def addReference(referencedFrom: CacheType, _endpointName: String = "", _index: Int = -1) = {
    if (_endpointName.length > 0) {
      if (endpointName.length > 0)
        warnings ::= "point '" + name + "' is referenced by two endpoints: '" + endpointName + "' and '" + _endpointName + "'"
      endpointName = _endpointName
    }

    if (_index >= 0) {
      if (index >= 0 && index != _index)
        warnings ::= "Control '" + name + "' has two index values in the configuration: '" + index + "' and '" + _index + "'"
      index = _index
    }

    incrementReference(referencedFrom)
  }

}
