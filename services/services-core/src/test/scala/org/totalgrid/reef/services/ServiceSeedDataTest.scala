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
package org.totalgrid.reef.services

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.models.{ ApplicationSchema, DatabaseUsingTestBase }
import org.squeryl.{ Table, KeyedEntity }

@RunWith(classOf[JUnitRunner])
class ServiceSeedDataTest extends DatabaseUsingTestBase {

  import org.squeryl.PrimitiveTypeMode._
  def count() = {

    val eventConfigs = ApplicationSchema.eventConfigs.where(t => true === true).size
    val agents = ApplicationSchema.agents.where(t => true === true).size
    val entityTypes = ApplicationSchema.entityTypeMetaModel.where(t => true === true).size
    (eventConfigs, agents, entityTypes)
  }

  def agentPasswords() = {
    ApplicationSchema.agents.where(t => true === true).toList.map { a => a.digest + a.salt }
  }

  def deleteSome[A <: KeyedEntity[Long]](table: Table[A], count: Int) {
    val deleteConfigs = table.where(t => true === true).toList.slice(0, count)
    table.deleteWhere(_.id in deleteConfigs.map { _.id })
  }
  def deleteSomeS[A <: KeyedEntity[String]](table: Table[A], count: Int) {
    val deleteConfigs = table.where(t => true === true).toList.slice(0, count)
    table.deleteWhere(_.id in deleteConfigs.map { _.id })
  }

  val context = new SilentRequestContext

  test("Seed twice") {
    ServiceSeedData.seed(context, "system")
    val original = count()
    ServiceSeedData.seed(context, "system")
    original should equal(count())
  }

  test("Seed twice password unchanged") {
    ServiceSeedData.seed(context, "system")
    val original = agentPasswords()
    ServiceSeedData.seed(context, "second")
    original should equal(agentPasswords())
  }

  test("Partial Update needed") {
    ServiceSeedData.seed(context, "system")
    val original = count()
    deleteSome(ApplicationSchema.eventConfigs, 5)
    deleteSomeS(ApplicationSchema.entityTypeMetaModel, 5)
    deleteSome(ApplicationSchema.agents, 5)
    ServiceSeedData.seed(context, "system")
    original should equal(count())
  }
}
