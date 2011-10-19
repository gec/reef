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
package org.totalgrid.reef.api.protocol.api

import org.totalgrid.reef.api.proto.FEP.CommChannel
import com.weiglewilczek.slf4s.Logging

trait ChannelAlwaysOnline extends Protocol with Logging {

  import Protocol._

  private val channelMap = scala.collection.mutable.Map.empty[String, ChannelPublisher]

  abstract override def addChannel(p: CommChannel, publisher: ChannelPublisher): Unit = {
    super.addChannel(p, publisher)
    channelMap += p.getName -> publisher
    publisher.publish(CommChannel.State.OPENING)
    publisher.publish(CommChannel.State.OPEN)
  }

  abstract override def removeChannel(name: String): Unit = {
    val ret = super.removeChannel(name)
    channelMap.remove(name) match {
      case Some(x) => x.publish(CommChannel.State.CLOSED)
      case None => logger.error("Referenced channel not in map: " + name)
    }
  }

}
