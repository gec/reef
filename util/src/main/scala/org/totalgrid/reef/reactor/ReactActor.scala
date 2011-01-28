/**
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.reactor

import scala.actors.{ AbstractActor }
import scala.actors.Actor._

/**
 *  Implements Reactor as a concete Actor using react style
 *  message handling (thread pool)   
 *  
 */
trait ReactActor extends Reactor with Lifecycle {

  val parentclass = this

  def getReactableActor: ReactorBase = new ReactorBase {

    def beforeExit = beforeStop()

    val reactable = parentclass

    def act {
      loop {
        react {
          this.mainPartial
        }
      }
    }

  }

}