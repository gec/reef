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
package org.totalgrid.reef.httpbridge

/**
 * magic strings in the JSON bridge code
 */
object JsonBridgeConstants {

  // Custom Http Headers we proxy through
  val AUTH_HEADER = "REEF_AUTH_TOKEN"
  val VERB_HEADER = "REEF_VERB"
  val TIMEOUT_HEADER = "TIMEOUT_MILLIS"
  val RESULT_LIMIT_HEADER = "RESULT_LIMIT"
  val RETURN_STYLE = "REEF_RETURN_STYLE"

  // all custom headers (used for CORS support)
  val CUSTOM_HEADERS = List(AUTH_HEADER, VERB_HEADER, TIMEOUT_HEADER, RESULT_LIMIT_HEADER, RETURN_STYLE)

  // GET /login parameters
  val NAME_PARAMETER = "name"
  val PASSWORD_PARAMETER = "password"

  // standard HTTP Headers
  val CONTENT_TYPE_HEADER = "Content-Type"
  val ACCEPT_HEADER = "Accept"
  val ORIGIN_HEADER = "Access-Control-Allow-Origin"

  // acceptable input/output formats
  val JSON_FORMAT = "application/json"
  val PROTOBUF_FORMAT = "application/protobuf"

  val VALID_FORMATS = List(JSON_FORMAT, PROTOBUF_FORMAT)

  val TEXT_FORMAT = "text/plain"
}