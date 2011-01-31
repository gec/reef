/**
 * Copyright 2011 Green Energy Corp.
 *
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
package org.totalgrid.reef.messaging

import com.google.protobuf.GeneratedMessage

object AMQPConvertingProtoPublisher {
  def wrapSend[T <: GeneratedMessage](sendFun: (Array[Byte], String) => Unit, keygen: T => String): T => Unit = { (value: T) =>
    {
      sendFun(ProtoSerializer.convertProtoToBytes(value), keygen(value))
    }
  }
  def wrapSendWithKey[T <: GeneratedMessage](sendFun: (Array[Byte], String) => Unit): (T, String) => Unit = { (value: T, key: String) =>
    {
      sendFun(ProtoSerializer.convertProtoToBytes(value), key)
    }
  }

  def wrapSendToExchange[T <: GeneratedMessage](sendFun: (Array[Byte], String, String) => Unit): (T, String, String) => Unit = { (value: T, exchange: String, key: String) =>
    {
      sendFun(ProtoSerializer.convertProtoToBytes(value), exchange, key)
    }
  }
}
