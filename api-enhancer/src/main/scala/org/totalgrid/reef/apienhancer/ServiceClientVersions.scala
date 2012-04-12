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
package org.totalgrid.reef.apienhancer

import java.io.File
import com.sun.javadoc.{ LanguageVersion, RootDoc }

object ServiceClientVersions {
  def start(root: RootDoc): Boolean = {

    // we use the destDir setting in javadoc
    val rootDir = new File(".")
    val sourceDir = new File(rootDir, "../../src/main/java")

    val transformers = List(
      new ScalaTraits(true),
      new ScalaTraits(false),
      new ScalaJavaShims(false),
      new JavaFutures,
      new ScalaJavaShims(true),
      new ScalaSyncShims)

    ApiTransformer.generateFiles(root, sourceDir, rootDir, transformers)
  }

  /**
   * FROM: http://stackoverflow.com/questions/5731619/doclet-get-generics-of-a-list
   * vote first answer up if you read this comment
   * NOTE: Without this method present and returning LanguageVersion.JAVA_1_5,
   *       Javadoc will not process generics because it assumes LanguageVersion.JAVA_1_1
   * @return language version (hard coded to LanguageVersion.JAVA_1_5)
   */
  def languageVersion() = {
    LanguageVersion.JAVA_1_5;
  }
}
