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
package org.totalgrid.reef.protocol.api

import org.totalgrid.reef.proto.FEP.CommChannel

trait ChannelAlwaysOnline extends Protocol {

  abstract override def addChannel(p: CommChannel, listener: Listener[CommChannel.State]): Unit = {
    super.addChannel(p, listener)
    listener.onUpdate(CommChannel.State.OPENING)
    listener.onUpdate(CommChannel.State.OPEN)
  }

  abstract override def removeChannel(name: String): Listener[CommChannel.State] = {
    val ret = super.removeChannel(name)
    ret.onUpdate(CommChannel.State.CLOSED)
    ret
  }

}
