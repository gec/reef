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
package org.totalgrid.reef.services.metrics

import org.totalgrid.reef.metrics.{ StaticMetricsHooksBase, MetricsHookSource }
import org.totalgrid.reef.services.framework.RequestContext
import org.totalgrid.reef.services.authz.AuthzService
import java.util.UUID

class AuthzServiceMetricsWrapper(authz: AuthzService, source: MetricsHookSource) extends StaticMetricsHooksBase(source) with AuthzService {
  private val prepareCount = counterHook("PrepareCount")
  private val prepareTime = timingHook("PrepareTime")
  private val authCount = counterHook("AuthCount")
  private val authTime = timingHook("AuthTime")
  private val filterCount = counterHook("FilterCount")
  private val filterTime = timingHook("FilterTime")

  override def filter[A](context: RequestContext, componentId: String, action: String, payload: List[A], uuids: => List[List[UUID]]) = {
    filterCount(1)
    filterTime {
      authz.filter(context, componentId, action, payload, uuids)
    }
  }

  override def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {
    authCount(1)
    authTime {
      authz.authorize(context, componentId, action, uuids)
    }
  }

  override def selector(context: RequestContext, componentId: String, action: String) = {
    authz.selector(context, componentId, action)
  }
  def visibilityMap(context: RequestContext) = authz.visibilityMap(context)

  override def prepare(context: RequestContext) = {
    prepareCount(1)
    prepareTime {
      authz.prepare(context)
    }
  }
}