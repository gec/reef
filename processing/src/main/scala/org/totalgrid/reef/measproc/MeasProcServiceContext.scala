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

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.app.{ SubscriptionHandlerBase, ServiceContext }

// Tailored for some of the measproc classes
trait MeasProcServiceContext[A] extends ServiceContext[A] with SubscriptionHandlerBase[A] with Logging {

  // define add, remove, and clear    
  // clear out your object cache
  def clear()

  //modified and subscribed get defined
  def subscribed(list: List[A]) = {
    logger.info("Subscribed")
    list.foreach { x => add(x) }
  }

  def modify(obj: A) = add(obj)

}

