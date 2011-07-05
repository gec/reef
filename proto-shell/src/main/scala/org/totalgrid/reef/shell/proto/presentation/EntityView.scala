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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Model.{ Entity }
import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Table

object EntityView {

  def printInspect(ent: Entity) = {
    val lines =
      ("uid" :: ent.getUuid.getUuid :: Nil) ::
        ("name" :: ent.getName :: Nil) ::
        ("types" :: "(" + ent.getTypesList.toList.mkString(", ") + ")" :: Nil) ::
        Nil

    Table.justifyColumns(lines).foreach(line => println(line.mkString(" | ")))
  }

  def printList(ents: List[Entity]) = {
    println("Found: " + ents.length)
    val lines = ents.map(EntityView.toLine(_))
    val justified = Table.justifyColumns(lines)

    val rowLength = Table.rowLength(justified.head) + 2
    println("".padTo(rowLength, "-").mkString)
    justified.foreach(line => println(line.mkString(" ")))
  }

  def toLine(ent: Entity): List[String] = {
    "[" + ent.getUuid.getUuid + "]" ::
      ent.getName ::
      "(" + ent.getTypesList.toList.mkString(", ") + ")" ::
      Nil
  }

  def printTreeSingleDepth(root: Entity) {
    val margin = " "
    val childLines = root.getRelationsList.toList.flatMap { rel =>
      rel.getEntitiesList.toList.map { ent =>
        "|--" :: toLine(ent)
      }
    }

    val justChildren = Table.justifyColumns(childLines)

    val rootLine = "+" :: toLine(root)
    (rootLine :: justChildren).foreach(line => println(margin + line.mkString(" ")))
  }

  def printTreeMultiDepth(root: Entity) {
    val margin = " "
    val childLines = root.getRelationsList.toList.flatMap { rel =>
      rel.getEntitiesList.toList.map { ent =>
        "|-(" + rel.getDistance + ")-" :: toLine(ent)
      }
    }

    val justChildren = Table.justifyColumns(childLines)

    val rootLine = "+" :: toLine(root)
    (rootLine :: justChildren).foreach(line => println(margin + line.mkString(" ")))
  }

  def printTreeRecursively(root: Entity) {

    def getSubTree(root: Entity, depth: Int): List[List[String]] = {

      val tag = if (root.getRelationsCount > 0 || depth == 0) "+" else "|"
      val rootLine: List[String] = "  " * depth :: (tag + "-") :: toLine(root)

      val childLines: List[List[String]] = root.getRelationsList.toList.flatMap { rel =>
        rel.getEntitiesList.toList.map { ent =>
          getSubTree(ent, depth + 1)
        }
      }.flatten
      (rootLine :: childLines)

    }

    val margin = " "

    val treeLines = getSubTree(root, 0)

    treeLines.foreach(line => println(margin + line.mkString(" ")))
  }
}