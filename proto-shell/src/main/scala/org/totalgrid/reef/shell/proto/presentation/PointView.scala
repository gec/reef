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

import org.totalgrid.reef.api.proto.Model.{ Entity, Point }
import org.totalgrid.reef.api.proto.OptionalProtos._
import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Table

object PointView {
  def printPointTable(points: List[Point]) = {
    Table.printTable(pointHeader, points.map(pointRow(_)))
  }

  def pointHeader = {
    "Name" :: "Type" :: "Unit" :: "Endpoint" :: Nil
  }

  def pointRow(a: Point) = {
    a.getName ::
      a.getType.toString ::
      a.getUnit ::
      a.logicalNode.name.getOrElse("") ::
      Nil
  }

  def printPointsWithCommands(entities: List[Entity]) = {
    Table.printTable(pointCommandHeader, entities.map(pointCommandRow(_)))
  }

  def pointCommandHeader = {
    "Name" :: "Commands" :: Nil
  }

  def pointCommandRow(a: Entity) = {
    a.getName ::
      a.getRelationsList.toList.map { _.getEntitiesList.toList.map { _.getName } }.flatten.mkString(", ") ::
      Nil
  }

}