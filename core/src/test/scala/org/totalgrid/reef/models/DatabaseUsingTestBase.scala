package org.totalgrid.reef.models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }
import org.totalgrid.reef.services.ServiceBootstrap

abstract class DatabaseUsingTestBase extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }
  override def beforeEach() {
    ServiceBootstrap.resetDb
    ServiceBootstrap.seed
  }
}