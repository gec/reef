package org.totalgrid.reef.measurementstore

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
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.executor.Executor
import scala.collection.JavaConversions._
import org.osgi.framework.{ ServiceReference, BundleContext }
import javax.management.remote.rmi._RMIConnection_Stub

object MeasurementStoreFinder extends Logging {

  import org.totalgrid.reef.executor.{ Lifecycle, ReactActorExecutor }
  import org.totalgrid.reef.util.BuildEnv.ConnInfo
  import org.totalgrid.reef.persistence.squeryl._
  /**
   * Get a measurement store implementation depending on the system configuration
   * @param lifecyleSink if the store generates any Lifecycle objects throw them here TODO: fix with with DI?
   * @return measurement store
   */
  /*def getInstance(config: ConnInfo, lifecyleSink: Lifecycle => Unit): MeasurementStore = {
    null
  }*/

  def getInstance(config: ConnInfo, executor: Executor, context: BundleContext): MeasurementStore = {
    config match {
      case di: DbInfo =>

        //val services = Option(context.getServiceReferences(classOf[MeasurementStoreFactory].getName, "(org.totalgrid.reef.mstore=sql)")).getOrElse(Nil)
        val services: List[ServiceReference] = Option(context.getServiceReferences(classOf[MeasurementStoreFactory].getName, "(org.totalgrid.reef.mstore=sql)")).map(_.toList).getOrElse(Nil)
        println("Services: " + services)
        services.headOption match {
          case Some(srvRef) =>
            val factory = context.getService(srvRef).asInstanceOf[MeasurementStoreFactory]
            val connFun = () => DbConnector.connect(di, context)
            val dbOps = new DbOperations(connFun, executor)(x => logger.info("connected to db: " + x))
            factory.buildStore(dbOps)
          case None => throw new Exception("SQL Measurement Store not found")
        }
      case _ => throw new Exception("Unknown measurementStore Implementation: " + config)
    }
  }

  def getInstance(config: ConnInfo, lifecyleSink: Lifecycle => Unit): MeasurementStore = {
    config match {
      case di: DbInfo =>
        null
      /*val actor = new ReactActorExecutor {}
        val connection = new SimpleDbConnection(di, actor)(x => logger.info("connected to db: " + x))
        lifecyleSink(actor)
        new SqlMeasurementStore(connection)*/
      case _ => throw new Exception("Unknown measurementStore Implementation: " + config)
    }
  }

  /**
   * loads the default measurement store configuration information
   */
  def getConfig(): ConnInfo = {
    val measurementStore = Option(System.getProperty("measurement_store")) getOrElse {
      throw new Exception("Measurement store not specified")
    }
    measurementStore match {
      case "sql" => DbInfo.loadInfo
      case _ => throw new Exception("Unknown measurementStore Implementation: " + measurementStore)
    }
  }
}