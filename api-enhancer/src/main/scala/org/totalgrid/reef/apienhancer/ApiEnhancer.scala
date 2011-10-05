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
import scala.collection.Map

object ApiEnhancer {
  def start(root: RootDoc): Boolean = {

    val rootDir = new File("c:/code/scada/reef/api-request/target/generated-sources")

    root.classes.toList.filter { c =>
      c.containingPackage.toString.indexOf(".japi.request") != -1 &&
        c.qualifiedName.toString.indexOf("AllScadaService") == -1
    }.foreach { c =>
      val packageStr = c.containingPackage().toString
      makeJavaClass(c, packageStr, rootDir)

      makeJavaFuture(c, packageStr, rootDir)

      makeScalaPackage(c, packageStr, rootDir)

      makeJavaShim(c, packageStr, rootDir)
    }

    true
  }

  private def makeJavaClass(c: ClassDoc, packageStr: String, rootDir: File) {
    val javaPackage = packageStr.replaceAllLiterally(".japi.", ".japi2.")
    val packageDir = new File(rootDir, "java/" + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, c.name + ".java")
    println(classFile.getAbsolutePath)
    val stream = new PrintStream(new FileOutputStream(classFile))
    duplicateJava(c, stream, javaPackage)
    stream.close
  }

  private def duplicateJava(c: ClassDoc, stream: PrintStream, packageName: String) {
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

  private def makeJavaFuture(c: ClassDoc, packageStr: String, rootDir: File) {
    val javaPackage = packageStr.replaceAllLiterally(".japi.", ".japiF.")
    val packageDir = new File(rootDir, "java/" + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, c.name + "Futures.java")
    println(classFile.getAbsolutePath)
    val stream = new PrintStream(new FileOutputStream(classFile))
    javaFuture(c, stream, javaPackage)
    stream.close
  }

  private def javaFuture(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")
    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName() + ";"))
    stream.println("import org.totalgrid.reef.promise.Promise;")
    stream.println(commentString(c.getRawCommentText()))
    stream.println("public interface " + c.name + "Futures" + "{")

    c.methods.toList.foreach { m =>

      var msg = "\t" + "Promise<" + typeString(m.returnType) + "> " + m.name + "("
      msg += m.parameters().toList.map { p =>
        typeString(p.`type`) + " " + p.name
      }.mkString(", ")
      msg += ") throws ReefServiceException;"
      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }

    stream.println("}")
  }

  private def makeScalaPackage(c: ClassDoc, packageStr: String, rootDir: File) {
    val javaPackage = packageStr.replaceAllLiterally(".japi.", ".sapi.")
    val packageDir = new File(rootDir, "scala/" + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, c.name + ".scala")
    println(classFile.getAbsolutePath)
    val stream = new PrintStream(new FileOutputStream(classFile))
    scalaClass(c, stream, javaPackage)
    stream.close
  }

  private def scalaClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")

    val importMap = Map("java.util.List" -> "")

    c.importedClasses().toList.foreach(p => importMap.get(p.qualifiedTypeName()) match {
      case None => stream.println("import " + p.qualifiedTypeName())
      case _ =>
    })
    stream.println("import org.totalgrid.reef.promise.Promise;")
    stream.println(commentString(c.getRawCommentText()))
    stream.println("trait " + c.name + "{")

    c.methods.toList.foreach { m =>

      var msg = "\t" + "def " + m.name + "("
      msg += m.parameters().toList.map { p =>
        p.name + ": " + scalaTypeString(p.`type`)
      }.mkString(", ")
      msg += ")"
      if (m.returnType.toString != "void")
        msg += ": Promise[" + scalaTypeString(m.returnType) + "]"

      stream.println(commentString(m.getRawCommentText()))
      stream.println(msg)
    }

    stream.println("}")
  }

  private def makeJavaShim(c: ClassDoc, packageStr: String, rootDir: File) {
    val javaPackage = packageStr.replaceAllLiterally(".japi.request", ".sapi.request.impl")
    val packageDir = new File(rootDir, "scala/" + javaPackage.replaceAllLiterally(".", "/"))
    packageDir.mkdirs()

    val classFile = new File(packageDir, c.name + "JavaShim.scala")
    println(classFile.getAbsolutePath)
    val stream = new PrintStream(new FileOutputStream(classFile))
    javaShimClass(c, stream, javaPackage)
    stream.close
  }

  private def javaShimClass(c: ClassDoc, stream: PrintStream, packageName: String) {
    stream.println("package " + packageName + ";")

    val importMap = Map("java.util.List" -> "")

    c.importedClasses().toList.foreach(p => stream.println("import " + p.qualifiedTypeName()))
    stream.println("import scala.collection.JavaConversions._")
    stream.println("import org.totalgrid.reef.japi.request.impl.Converters._")
    stream.println("import org.totalgrid.reef.japi.request.{" + c.name + "=> JInterface }")
    stream.println("import org.totalgrid.reef.sapi.request." + c.name)
    stream.println("import org.totalgrid.reef.japi.request.impl.AllScadaServiceImpl")

    stream.println("trait " + c.name + "JavaShim extends JInterface{")

    stream.println("\tdef service: AllScadaServiceImpl")

    c.methods.toList.foreach { m =>

      var msg = "\t" + "override def " + m.name + "("
      msg += m.parameters().toList.map { p =>
        p.name + ": " + javaAsScalaTypeString(p.`type`)
      }.mkString(", ")
      msg += ")"
      if (m.returnType.toString != "void")
        msg += ": " + javaAsScalaTypeString(m.returnType)

      msg += " = "
      if (m.returnType.simpleTypeName() == "SubscriptionResult") msg += "convert("
      msg += "service." + m.name + "("
      msg += m.parameters().toList.map { p =>
        if (p.`type`().simpleTypeName == "List") p.name + ".toList"
        else p.name
      }.mkString(", ")
      msg += ").await()"
      if (m.returnType.simpleTypeName() == "SubscriptionResult") msg += ")"
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
      ptype.simpleTypeName() + ptype.dimension()
    }
  }

  private def scalaTypeString(ptype: Type): String = {

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

  private def javaAsScalaTypeString(ptype: Type): String = {

    val map = Map(
      "int" -> "Int",
      "boolean" -> "Boolean",
      "long" -> "Long",
      "double" -> "Double",
      "byte" -> "Array[Byte]", "List" -> "java.util.List")

    if (ptype.asParameterizedType() != null) {

      val argumentTypes = ptype.asParameterizedType().typeArguments().toList
      ptype.simpleTypeName() + "[ " + argumentTypes.map { javaAsScalaTypeString(_) }.mkString(", ") + "]"
    } else {
      val simpleType = ptype.simpleTypeName()
      map.get(simpleType).getOrElse(simpleType)
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
