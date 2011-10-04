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

import scala.collection.JavaConversions._
import java.io.{ FileOutputStream, File, PrintStream }

object ApiEnhancer {
  def start(root: RootDoc): Boolean = {

    val rootDir = new File("c:/code/scada/reef/api-request/src/main/java")

    root.classes.toList.foreach { c =>
      val packageStr = c.containingPackage().toString.replaceAllLiterally(".japi.", ".japi2.")
      val packageDir = new File(rootDir, packageStr.replaceAllLiterally(".", "/"))
      packageDir.mkdirs()

      val classFile = new File(packageDir, c.name + ".java")
      val stream = new PrintStream(new FileOutputStream(classFile))
      handleClass(c, stream, packageStr)
      stream.close
    }

    true
  }

  private def handleClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")
    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName() + ";"))
    stream.println(commentString(c.getRawCommentText()))
    stream.println("public interface " + c.name + "{")

    c.methods.toList.foreach { m =>

      var msg = "\t" + typeString(m.returnType) + " " + m.name + "("
      msg += m.parameters().toList.map { p =>
        typeString(p.`type`) + " " + p.name
      }.mkString(", ")
      msg += ") throws ReefServiceException;"
      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }

    stream.println("}")
  }

  private def commentString(commentText: String): String = {
    "/**\n" + commentText.lines.toList.map { " *" + _ }.mkString("\n") + "\n*/"
  }

  private def typeString(ptype: Type): String = {
    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.typeName() + "< " + argumentTypes.map { typeString(_) }.mkString(", ") + " >"
    } else {
      ptype.simpleTypeName()
    }
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
