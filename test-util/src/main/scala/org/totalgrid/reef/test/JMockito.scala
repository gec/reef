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
package org.totalgrid.test

import org.mockito.Matchers.{ eq => jeq }
import org.mockito.Matchers.{ contains => jcontains }
import org.mockito.{ ArgumentCaptor }

trait JMockito {
  def jMockitoEq[T](value: T): T =
    {
      jeq(value)
    }

  def jMockitoContains(substring: String): String =
    {
      jcontains(substring)
    }

  def createArgumentCaptor[T](implicit classManifest: ClassManifest[T]): ArgumentCaptor[T] = ArgumentCaptor.forClass(classManifest.erasure)
    .asInstanceOf[ArgumentCaptor[T]]

  def never() =
    {
      org.mockito.Mockito.never()
    }

  def times(count: Int) =
    {
      org.mockito.Mockito.times(count)
    }

}