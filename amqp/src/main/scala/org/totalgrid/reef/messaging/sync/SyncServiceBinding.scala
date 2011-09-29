/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.messaging.sync

import org.totalgrid.reef.messaging.QueuePatterns
import org.totalgrid.reef.sapi.Routable
import org.totalgrid.reef.broker.api.{ CloseableChannel, MessageConsumer, BrokerChannel }

/**
 * synchronous service binding, useful for testing, not ready for production use until we have a good
 * synchronous restarting policy
 */
class SyncServiceBinding(channel: BrokerChannel, exchange: String, destination: Routable, competing: Boolean, mc: MessageConsumer) extends CloseableChannel {

  if (competing)
    QueuePatterns.getCompetingConsumer(channel, exchange, exchange + "_server", destination.key, mc)
  else
    QueuePatterns.getExclusiveQueue(channel, exchange, destination.key, mc)

  def close() = channel.close

}
