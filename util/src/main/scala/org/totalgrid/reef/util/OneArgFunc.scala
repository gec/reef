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
package org.totalgrid.reef.util

object OneArgFunc {

  def getParamClass[A](f: A => _, klass: Class[_]): Class[A] = {
    f.getClass.getDeclaredMethods.find { x =>
      x.getName.equals("apply") && x.getParameterTypes.size == 1 && x.getReturnType == klass
    } match {
      case None => throw new Exception("No such method")
      case Some(x) => x.getParameterTypes.apply(0).asInstanceOf[Class[A]]
    }
  }

  // takes any function that returns T, and returns a class representing it's type
  def getReturnClass[A](f: _ => A, klass: Class[_]): Class[A] = {
    f.getClass.getDeclaredMethod("apply", klass).getReturnType.asInstanceOf[Class[A]]
  }
}
