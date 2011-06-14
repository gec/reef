/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core.util

import org.totalgrid.reef.proto.Utils
import com.google.protobuf.ByteString
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._ // toList

/**
 * List of name-value pairs.
 *
 * <h3>Goals</h3>
 *
 *
 * <h3>Use Cases</h3>
 * <b> Receive an AttributeList proto, add an attribute, and send it out again.
 *
 * <pre>
 * val al = new AttributeList( attributeListProto)
 * val longitude = al.applyDouble("longitude")
 * val latitude  = al.applyDouble("latitude")  ...
 *
 * al += AttributeBoolean( "isAHit", isAHit( longitude, latitude))
 * publish( al.toProto)
 * </pre>
 *
 * @author flint
 */

/**
 * The type enum for a value
 */
abstract class ValueType(val id: Int)
object ValueType {
  import org.totalgrid.reef.proto.Utils.Attribute.Type

  case object STRING extends ValueType(Type.STRING.getNumber)
  case object LONG extends ValueType(Type.SINT64.getNumber)
  case object DOUBLE extends ValueType(Type.DOUBLE.getNumber)
  case object BOOLEAN extends ValueType(Type.BOOL.getNumber)
  case object BYTES extends ValueType(Type.BYTES.getNumber)
}

/**
 * A single attribute-value pair.
 */
abstract class Attribute(val vtype: ValueType, val vdescriptor: Option[String]) {

  def getString: String = throw new IllegalArgumentException("not a String")
  def getLong: Long = throw new IllegalArgumentException("not a Long")
  def getDouble: Double = throw new IllegalArgumentException("not a Double")
  def getBoolean: Boolean = throw new IllegalArgumentException("not a Boolean")
  def getByteArray: Array[Byte] = throw new IllegalArgumentException("not a Byte Array")
}

case class AttributeString(val value: String, override val vdescriptor: Option[String] = None) extends Attribute(ValueType.STRING, vdescriptor) {
  override def getString = value
}
case class AttributeLong(val value: Long, override val vdescriptor: Option[String] = None) extends Attribute(ValueType.LONG, vdescriptor) {
  override def getLong = value
  override def getString = value.toString
}
case class AttributeDouble(val value: Double, override val vdescriptor: Option[String] = None) extends Attribute(ValueType.DOUBLE, vdescriptor) {
  override def getDouble = value
  override def getString = value.toString
}
case class AttributeBoolean(val value: Boolean, override val vdescriptor: Option[String] = None) extends Attribute(ValueType.BOOLEAN, vdescriptor) {
  override def getBoolean = value
  override def getString = value.toString
}
case class AttributeByteArray(val value: Array[Byte], override val vdescriptor: Option[String] = None) extends Attribute(ValueType.BYTES, vdescriptor) {
  override def getByteArray = value
  override def getString = value.toString // TODO: should we throw an exception on getString?
}

/**
 *  A list of attribute-value pairs with conversions to and
 *  from reef.proto.Utils.AttributeList.
 */
class AttributeList extends HashMap[String, Attribute] {

  /**
   *  Construct an AttributeList from a reef.proto.Utils.AttributeList
   */
  def this(alist: Utils.AttributeList) = {
    this() // invoke primary constructor
    import ValueType._
    alist.getAttributeList().toList.map(a => {
      val name = a.getName
      val vtype = a.getVtype.getNumber
      var vdescriptor: Option[String] = None
      if (a.hasVdescriptor)
        vdescriptor = Some(a.getVdescriptor)
      //val vdescriptor = a.vdescriptor // Option[String]
      vtype match {
        case STRING.id => this += (name -> AttributeString(a.getValueString, vdescriptor))
        case LONG.id => this += (name -> AttributeLong(a.getValueSint64, vdescriptor))
        case DOUBLE.id => this += (name -> AttributeDouble(a.getValueDouble, vdescriptor))
        case BOOLEAN.id => this += (name -> AttributeBoolean(a.getValueBool, vdescriptor))
        case BYTES.id => this += (name -> AttributeByteArray(a.getValueBytes.toByteArray, vdescriptor))
      }
    })

  }

  def addAttribute(name: String, value: Any): AttributeList = {
    value match {
      case x: Int => this += (name -> AttributeLong(x))
      case x: Long => this += (name -> AttributeLong(x))
      case x: Float => this += (name -> AttributeDouble(x))
      case x: Double => this += (name -> AttributeDouble(x))
      case x: Boolean => this += (name -> AttributeBoolean(x))
      case x: Array[Byte] => this += (name -> AttributeByteArray(x))
      case x: String => this += (name -> AttributeString(x))
      case _ => throw new IllegalArgumentException("Unknown attribute type for class: " + value.asInstanceOf[AnyRef].getClass)
    }
    this
  }

  /**
   * Get a value with a specific type
   */
  def applyString(name: String): String = apply(name).getString
  def applyLong(name: String): Long = apply(name).getLong
  def applyDouble(name: String): Double = apply(name).getDouble
  def applyBoolean(name: String): Boolean = apply(name).getBoolean
  def applyByteArray(name: String): Array[Byte] = apply(name).getByteArray

  /**
   * Convert to proto. This does not call build()
   */
  def toProto: Utils.AttributeList.Builder = {
    val alist = Utils.AttributeList.newBuilder
    for ((key, value) <- this) {
      val attribute = Utils.Attribute.newBuilder
      attribute.setName(key)
      attribute.setVtype(Utils.Attribute.Type.valueOf(value.vtype.id))
      value.vdescriptor match {
        case Some(vdescriptor) => attribute.setVdescriptor(vdescriptor)
        case None =>
      }
      value match {
        case AttributeString(v, vdescriptor) => attribute.setValueString(v)
        case AttributeLong(v, vdescriptor) => attribute.setValueSint64(v)
        case AttributeDouble(v, vdescriptor) => attribute.setValueDouble(v)
        case AttributeBoolean(v, vdescriptor) => attribute.setValueBool(v)
        case AttributeByteArray(v, vdescriptor) => attribute.setValueBytes(ByteString.copyFrom(v))
      }
      alist.addAttribute(attribute)
    }
    alist //.build
  }
}
