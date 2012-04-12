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

import com.sun.javadoc.{ MethodDoc, Type }
import java.io.{ FileOutputStream, PrintStream, File }

trait GeneratorFunctions {

  def getFileStream(packageStr: String, outputDir: File, sourceFile: File, newPackage: String, scalaFile: Boolean, className: String)(func: (PrintStream, String) => Unit) {
    val javaPackage = packageStr.replaceAllLiterally(".client.service", newPackage)

    val (fileEnding, folder) = if (scalaFile) (".scala", "scala/") else (".java", "java/")

    val packageDir = new File(outputDir, folder + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, className + fileEnding)

    writeFileIfNewer(classFile, sourceFile.lastModified) { stream =>
      func(stream, javaPackage)
    }
  }

  def writeFileIfNewer(outputFile: File, shouldBeNewerThan: Long)(func: (PrintStream) => Unit) {
    if (!outputFile.exists || shouldBeNewerThan > outputFile.lastModified) {
      println("Genenerating: " + outputFile.getAbsolutePath)
      outputFile.getParentFile.mkdirs()
      val stream = new PrintStream(new FileOutputStream(outputFile))
      func(stream)
      stream.close
    } else {
      println("Skipping: " + outputFile.getAbsolutePath)
    }
  }

  def commentString(commentText: String, numTabs: Int = 0): String = {
    val tabs = (1 to numTabs).map { i => "\t" }.mkString("")
    val strippedText = commentText.replaceAllLiterally("!api-definition!", "")
    tabs + "/**\n" + strippedText.lines.toList.map { tabs + " *" + _ }.mkString("\n") + "\n" + tabs + "*/"
  }

  /**
   * get the type as a printable String => Measurement or List<Things>
   */
  def typeString(ptype: Type): String = {
    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.typeName() + "< " + argumentTypes.map { typeString(_) }.mkString(", ") + " >"
    } else {
      ptype.simpleTypeName() + ptype.dimension()
    }
  }

  /**
   * get the type parameter (if it exists) and render it as [Type] or <Type>
   */
  def typeAnnotation(m: MethodDoc, java: Boolean): String = {
    val typ = m.typeParameters().toList.headOption
    val typAnnotation = typ.map { t =>
      if (java) "<" + t.typeName() + "> " else "[" + t.typeName() + "] "
    }.getOrElse("")
    typAnnotation
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
      "List" -> "java.util.List",
      "Boolean" -> "java.lang.Boolean")

    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.simpleTypeName() + "[ " + argumentTypes.map { javaAsScalaTypeString(_) }.mkString(", ") + "]"
    } else {
      val simpleType = ptype.simpleTypeName()
      map.get(simpleType).getOrElse(simpleType)
    }
  }

  def isReturnOptional(m: MethodDoc) = {
    (m.name.startsWith("find") || m.name.startsWith("clear")) && m.returnType.simpleTypeName != "List"
  }

  def isReturnList(m: MethodDoc) = {
    m.returnType.simpleTypeName == "List"
  }

  def isReturnSubscription(m: MethodDoc) = {
    m.returnType.simpleTypeName == "SubscriptionResult"
  }
}