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
package org.totalgrid.reef.client.sapi.rpc.util

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.SubscriptionResult

object Converters {

  def convert[A](result: SubscriptionResult[List[A], A]) = {
    new SubscriptionResult[java.util.List[A], A] {
      def getSubscription = result.getSubscription

      def getResult = result.getResult
    }
  }

  def convert[A](result: Option[A]): A = {
    result.getOrElse(null.asInstanceOf[A])
  }

  def convert(result: scala.Boolean): java.lang.Boolean = {
    result.asInstanceOf[java.lang.Boolean]
  }
}