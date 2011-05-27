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
package org.totalgrid.reef.measproc

import scala.collection.immutable

class CompoundNotifier {
  class Handle(check: () => Unit) {
    var isReady = false
    def apply() = {
      isReady = true
      check()
    }
  }

  protected var list = immutable.List.empty[Handle]
  protected var fun: Option[() => Unit] = None
  protected var finished = false

  def notifier: () => Unit = {
    val h = new Handle(check)
    list ::= h
    h.apply
  }

  def observe(f: => Unit) = {
    fun = Some(() => f)
    check()
  }

  protected def check() {
    if (!finished && fun != None && list.forall(_.isReady)) {
      finished = true
      (fun.get)()
    }
  }

}