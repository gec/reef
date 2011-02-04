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
package org.totalgrid.reef.frontend

import scala.collection.JavaConversions._

import org.totalgrid.reef.util.{ Logging }
import scala.actors.Actor.actor

import org.totalgrid.reef.proto.FEP

import org.totalgrid.reef.proto.FEP.{ CommunicationEndpointConfig => ConfigProto, CommunicationEndpointConnection => ConnProto }

trait FEPOperations extends Logging {

  val services: FrontEndServices

  def load(ep: ConnProto)(resultFun: ConnProto => Unit): Unit = {
    load(ep :: Nil) { l => resultFun(l.head) }
  }

  def load(list: List[ConnProto])(next: List[ConnProto] => Unit) {

    services.config.asyncGetOneScatter(list.map { _.getEndpoint }) { blankConfigs =>
      loadConfig(blankConfigs) { configs =>

        val connections = list.zip(configs).map { x =>
          // maybe not the most efficient way to do this but certainly the most expressive
          ConnProto.newBuilder(x._1).setEndpoint(x._2).build
        }
        next(connections)
      }
    }

  }

  private def loadConfig(list: List[ConfigProto])(next: List[ConfigProto] => Unit) {

    val portsList = list.map { c => if (c.hasPort) Some(c.getPort) else None }.flatMap { t => t }
    services.port.asyncGetOneScatter(portsList) { ports =>
      val allConfig = list.map { cfg => cfg.getConfigFilesList.toList }.flatten
      services.file.asyncGetOneScatter(allConfig) { allConfigs =>
        val configs = list.map { c =>
          val ret = ConfigProto.newBuilder(c)
          ret.clearConfigFiles
          // TODO: find better algorithm for merging the results back in
          if (ret.hasPort) {
            ret.setPort(ports.find { p => c.getPort.getUid == p.getUid }.getOrElse(throw new Exception("Port uid:" + c.getPort.getUid + " not loaded")))
          }
          c.getConfigFilesList.toList.foreach { cf =>
            ret.addConfigFiles(allConfigs.find { p => cf.getUid == p.getUid }.getOrElse(throw new Exception("Config file uid:" + cf.getUid + " not loaded")))
          }
          ret.build
        }
        next(configs)
      }
    }
  }
}
