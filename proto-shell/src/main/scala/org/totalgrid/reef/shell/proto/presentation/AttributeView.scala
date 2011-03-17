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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Model.EntityAttributes
import org.totalgrid.reef.proto.Utils.Attribute

import scala.collection.JavaConversions._

object AttributeView {

  def printAttributes(attr: EntityAttributes) = {
    println(EntityView.toLine(attr.getEntity).mkString(" "))
    println("")
    if (attr.getAttributesCount > 0) {
      Table.justifyColumns(attr.getAttributesList.toList.map(toLine(_)))
        .foreach(line => println(line.mkString(": ")))
    } else {
      println("(No attributes)")
    }
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