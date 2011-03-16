package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Model.EntityAttributes
import org.totalgrid.reef.proto.Utils.Attribute

import scala.collection.JavaConversions._

object AttributeView {

  def printAttributes(attr: EntityAttributes) = {
    println(EntityView.toLine(attr.getEntity).mkString(" "))
    println("")
    Table.justifyColumns(attr.getAttributesList.toList.map(toLine(_)))
      .foreach(line => println(line.mkString(": ")))
  }

  def toLine(attr: Attribute) = {
    val v = attr.getVtype match {
      case Attribute.Type.BOOL => attr.getValueBool
      case Attribute.Type.STRING => attr.getValueString
      case Attribute.Type.SINT64 => attr.getValueSint64
      case Attribute.Type.DOUBLE => attr.getValueDouble
      case Attribute.Type.BYTES => {
        "data [length = " + attr.getValueBytes.size + "]"
      }
    }

    attr.getName :: v.toString :: Nil
  }
}