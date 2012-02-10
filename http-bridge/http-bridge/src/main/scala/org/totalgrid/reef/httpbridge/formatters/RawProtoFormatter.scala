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
package org.totalgrid.reef.httpbridge.formatters

import com.google.protobuf.Message
import java.io.InputStream
import org.totalgrid.reef.httpbridge.JsonBridgeConstants
import javax.servlet.ServletOutputStream

/**
 * a no-op formatter so we can input/output to standard binary proto
 * representations without special casing.
 */
class RawProtoFormatter extends SimpleFormatter {

  def mimeType = JsonBridgeConstants.PROTOBUF_FORMAT

  def output[A <: Message](stream: ServletOutputStream, list: List[A]) {
    list.map { proto => stream.write(proto.toByteArray) }
  }

  def outputOne[A <: Message](stream: ServletOutputStream, proto: A) {
    stream.write(proto.toByteArray)
  }

  def input[A <: Message.Builder](inputStream: InputStream, builder: A) {
    builder.mergeFrom(inputStream)
  }
}