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

import com.sun.javadoc._
import java.io.File

trait ApiTransformer {
  def make(c: ClassDoc, packageStr: String, outputDir: File, sourceFile: File)

  // called once all of the classes have been procesed
  def finish(rootDir: File) {}
}

object ApiTransformer {

  /**
   * @param root doclet collection of all classes we are processing
   * @param sourceDir base location of the api definition files
   * @param outputDir output dir for new packages
   * @param transformers list of transformers to run over the classes
   * @return
   */
  def generateFiles(root: RootDoc, sourceDir: File, outputDir: File, transformers: List[ApiTransformer]): Boolean = {
    root.classes.toList.filter { c =>
      c.getRawCommentText.indexOf("!api-definition!") != -1
    }.foreach { c =>

      val packageStr = c.containingPackage().toString

      val sourceFile = new File(sourceDir, c.qualifiedTypeName.replaceAllLiterally(".", "/") + ".java")
      if (!sourceFile.exists) throw new Exception("Can't find source file: " + sourceFile.getAbsolutePath)

      transformers.foreach { _.make(c, packageStr, outputDir, sourceFile) }
    }

    transformers.foreach { _.finish(outputDir) }

    true
  }
}
