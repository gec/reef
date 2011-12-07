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
package org.totalgrid.reef.loader.commons

import java.io.PrintStream

import scala.collection.JavaConversions._

import org.totalgrid.reef.loader.commons.ui.{ RequestViewer, SimpleTraversalProgressNotifier }
import org.totalgrid.reef.client.sapi.client.RequestSpy
import org.totalgrid.reef.client.service.proto.Model.Entity

object ModelDeleter {
  def deleteChildren(local: LoaderServices, roots: List[String], dryRun: Boolean, stream: Option[PrintStream], batchSize: Int = 25)(additionalDelete: (EquipmentModelTraverser, ModelCollector) => Unit): Long = {

    val cachingImporter = new EquipmentRemoverCache

    val traversalUi = stream.map { new SimpleTraversalProgressNotifier(_) }

    val traverser = new EquipmentModelTraverser(local, cachingImporter, traversalUi)

    stream.foreach { _.println("Finding items to delete starting at nodes: " + roots.mkString(", ")) }

    roots.foreach { name =>

      val rootOption = local.findEntityByName(name).await
      rootOption.foreach { root =>
        traverser.collect(root)
      }
    }

    additionalDelete(traverser, cachingImporter)

    traverser.finish()

    val itemsToDelete = cachingImporter.size

    if (dryRun) {
      stream.foreach { _.println("Skipping deletion of " + itemsToDelete + " objects") }
    } else {

      if (itemsToDelete > 0) {

        // wait for all endpoints to be disabled and stopped before deleting
        val endpoints = cachingImporter.endpoints
        if (!endpoints.isEmpty) EndpointStopper.stopEndpoints(local, endpoints, stream)

        val viewer = stream.map { new RequestViewer(_, cachingImporter.size) }
        RequestSpy.withRequestSpy(local, viewer) {
          cachingImporter.doDeletes(local, batchSize)
        }
        viewer.foreach { _.finish }

        stream.foreach { _.println("Deleted " + itemsToDelete + " objects successfully.") }
      } else {
        stream.foreach { _.println("Nothing to delete.") }
      }
    }
    itemsToDelete
  }

  def deleteEverything(local: LoaderServices, dryRun: Boolean, stream: Option[PrintStream], batchSize: Int = 25): Long = {
    deleteChildren(local, Nil, dryRun, stream, batchSize) { (traverse, collector) =>
      local.modifyHeaders(_.setResultLimit(50000))

      // since we never look at entities when we are deleting we dont need to look them up
      val fakeEntity = Entity.newBuilder.build

      local.getEndpoints().await.foreach { collector.addEndpoint(_, fakeEntity) }
      val types = "Site" :: "Root" :: "Region" :: "Equipment" :: "EquipmentGroup" :: Nil
      local.getEntitiesWithTypes(types).await.foreach { collector.addEquipment(_) }

      local.getCommunicationChannels().await.foreach { collector.addChannel(_, fakeEntity) }
      local.getConfigFiles().await.foreach { collector.addConfigFile(_, fakeEntity) }
      local.getPoints().await.foreach { collector.addPoint(_, fakeEntity) }
      local.getCommands().await.foreach { collector.addCommand(_, fakeEntity) }

      // TODO: add deleting of eventConfigurations
    }
  }

}