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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.Destination
import org.totalgrid.reef.protocol.api.Publisher
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.messaging.OrderedServiceTransmitter

class IdentityOrderedPublisher[A](
  tx: OrderedServiceTransmitter,
  verb: Envelope.Verb,
  address: Destination,
  maxRetries: Int) extends OrderedPublisher[A, A](tx, verb, address, maxRetries)(x => x)

class OrderedPublisher[A, B](
    tx: OrderedServiceTransmitter,
    verb: Envelope.Verb,
    address: Destination,
    maxRetries: Int)(transform: A => B) extends Publisher[A] {

  def publish(value: A): Promise[Boolean] = tx.publish(transform(value), verb, address, maxRetries)
}

