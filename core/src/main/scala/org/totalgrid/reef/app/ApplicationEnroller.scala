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
package org.totalgrid.reef.app

import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.Auth._

import org.totalgrid.reef.reactor.{ Reactable, Lifecycle }

import org.totalgrid.reef.api.{ ServiceHandlerHeaders, ServiceTypes, RequestEnv }
import ServiceTypes.{ Failure, SingleSuccess }

import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.messaging._
import org.totalgrid.reef.proto.ReefServicesList

object ApplicationEnroller extends Logging {

  def getSysProperty(prop: String, default: String): String = {
    val propVal = System.getProperty(prop)
    if (propVal == null) {
      info(prop + " not defined, defaulting to :" + default)
      default
    } else {
      propVal
    }
  }

  lazy val defaultUserName = getSysProperty("reef.user", "system")
  lazy val defaultUserPassword = getSysProperty("reef.user.password", "-system-")
  lazy val defaultNodeName = getSysProperty("reef.node", "node01")
  lazy val defaultLocation = getSysProperty("reef.network", "any")
  lazy val defaultNetwork = getSysProperty("reef.location", "any")

  def buildLogin(userName: Option[String] = None, userPassword: Option[String] = None) = {
    val agent = Agent.newBuilder
    agent.setName(userName.getOrElse(defaultUserName)).setPassword(userPassword.getOrElse(defaultUserPassword))
    val auth = AuthToken.newBuilder
    auth.setAgent(agent)
    auth.build
  }

  /// Use the system context to get user name, location, network and instanceName
  def buildConfig(capabilites: List[String], instanceName: Option[String] = None, userName: Option[String] = None, location: Option[String] = None, network: Option[String] = None): ApplicationConfig = {
    val b = ApplicationConfig.newBuilder()

    def randomString(n: Int): String = {
      var sb = new StringBuilder
      for (i <- 1 to n) sb.append(util.Random.nextPrintableChar)
      sb.toString
    }

    b.setInstanceName(instanceName.getOrElse(defaultNodeName + "-" + capabilites.mkString("-")))
    b.setUserName(userName.getOrElse(defaultUserName))
    b.setNetwork(network.getOrElse(defaultNetwork))
    b.setLocation(location.getOrElse(defaultLocation))
    capabilites.foreach(b.addCapabilites(_))
    b.setProcessId(randomString(8))
    b.build
  }
}
import ApplicationEnroller._

/**
 * handles the creation of the ApplicationConfig registration proto and then constructing the major components that
 * depend on the result of the registration (for output channels etc.).
 *
 * This object also handles the lifecycle of the
 *
 * @param amqp bus interface
 * @param processType should be either FEP or Processing
 * @param setupFun the construction function for the class using the components, must be StartStoppable
 */
abstract class ApplicationEnroller(amqp: AMQPProtoFactory, instanceName: Option[String], capabilites: List[String], setupFun: CoreApplicationComponents => Lifecycle) extends Reactable with Lifecycle with Logging {

  private var container: Option[Lifecycle] = None

  // we only need the registry to get the appClient, could special case for ApplicationConfig if bootstrapping exchange names
  private var client: Option[ProtoClient] = None

  private def enroll() {
    freshClient
    client.foreach(c =>
      c.asyncPutOne(buildLogin()) {
        _ match {
          case x: Failure =>
            error("Error getting auth token. " + x)
            delay(2000) { enroll() }
          case SingleSuccess(status, authToken) =>
            val env = new RequestEnv
            env.addAuthToken(authToken.getToken)
            c.setDefaultHeaders(env)
            c.asyncPutOne(buildConfig(capabilites, instanceName)) {
              _ match {
                case x: Failure =>
                  error("Error registering application. " + x)
                  delay(2000) { enroll() }
                case SingleSuccess(status, app) =>
                  val components = new CoreApplicationComponents(amqp, app, env)
                  container = Some(setupFun(components))
                  container.get.start
              }
            }
        }
      })
  }

  /**
   * there is an implementation detail of AMQP that states that the underlying "session" we use to communicate to the broker
   * will be closed if we try to publish a message to an exchange that hasn't been defined. In our system, if an FEP or HMI
   * client comes up before the services have started (like after a reboot) and an "exchange not found error" is reported we
   * need to throw away the client and get a new one for each attempt to talk to the auth service.
   */
  private def freshClient() {
    client.foreach(_.close)
    client = Some(new ProtoClient(amqp, ReefServicesList, 5000))
  }

  /**
   * blocking start
   */
  override def afterStart() = enroll()

  /**
   * stops the contained object
   */
  override def beforeStop() = {
    container.foreach(_.stop)
    client.foreach(_.close)
  }

}
