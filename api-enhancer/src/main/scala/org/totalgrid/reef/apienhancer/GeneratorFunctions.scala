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

import com.sun.javadoc.Type
import java.io.{ FileOutputStream, PrintStream, File }

trait GeneratorFunctions {

  def getFileStream(packageStr: String, rootDir: File, sourceFile: File, newPackage: String, scalaFile: Boolean, className: String)(func: (PrintStream, String) => Unit) {
    val javaPackage = packageStr.replaceAllLiterally(".japi.client.rpc", newPackage)

    val (fileEnding, folder) = if (scalaFile) (".scala", "scala/") else (".java", "java/")

    val packageDir = new File(rootDir, folder + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, className + fileEnding)

    if (!classFile.exists || sourceFile.lastModified > classFile.lastModified) {
      println("Genenerating: " + classFile.getAbsolutePath)
      val stream = new PrintStream(new FileOutputStream(classFile))
      func(stream, javaPackage)
      stream.close
    } else {
      println("Skipping: " + classFile.getAbsolutePath)
    }
  }

  def commentString(commentText: String): String = {
    "/**\n" + commentText.lines.toList.map { " *" + _ }.mkString("\n") + "\n*/"
  }

  def typeString(ptype: Type): String = {
    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.typeName() + "< " + argumentTypes.map { typeString(_) }.mkString(", ") + " >"
    } else {
      ptype.simpleTypeName() + ptype.dimension()
    }
  }

  def scalaTypeString(ptype: Type): String = {

    val map = Map(
      "int" -> "Int",
      "boolean" -> "Boolean",
      "long" -> "Long",
      "double" -> "Double",
      "byte" -> "Array[Byte]")

    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.typeName() + "[ " + argumentTypes.map { scalaTypeString(_) }.mkString(", ") + "]"
    } else {
      val simpleType = ptype.simpleTypeName()
      map.get(simpleType).getOrElse(simpleType)
    }
  }

  def javaAsScalaTypeString(ptype: Type): String = {

    val map = Map(
      "int" -> "Int",
      "boolean" -> "Boolean",
      "long" -> "Long",
      "double" -> "Double",
      "byte" -> "Array[Byte]",
      "List" -> "java.util.List")

    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.simpleTypeName() + "[ " + argumentTypes.map { javaAsScalaTypeString(_) }.mkString(", ") + "]"
    } else {
      val simpleType = ptype.simpleTypeName()
      map.get(simpleType).getOrElse(simpleType)
    }
  }
}