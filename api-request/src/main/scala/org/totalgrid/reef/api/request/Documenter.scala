/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConversions._
import com.google.protobuf.Descriptors.EnumValueDescriptor
import java.io.File
import xml.{ Node, XML, NodeSeq }
import org.totalgrid.reef.util.BuildEnv

class Documenter(file: String, title: String, desc: Node) {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  protected var usages = List.empty[NodeSeq]

  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: String, request: A, response: A): Unit = {
    addCase(title, verb, <div>{ desc }</div>, request, List(response))
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: String, request: A, responses: List[A]): Unit = {
    addCase(title, verb, <div>{ desc }</div>, request, responses)
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: Node, request: A, response: A): Unit = {
    usages ::= Documenter.document(title, verb, desc, request, List(response))
  }
  def addCase[A <: GeneratedMessage](title: String, verb: String, desc: Node, request: A, responses: List[A]): Unit = {
    usages ::= Documenter.document(title, verb, desc, request, responses)
  }

  def save = {
    val content =
      <servicedoc>
        <title>{ title }</title>
        <desc>{ desc }</desc>
        { usages.reverse }
      </servicedoc>

    val path = BuildEnv.configPath + "api-request/target/docxml"
    val dir = new File(path)
    dir.mkdirs

    XML.save(path + "/" + file, content, "UTF-8", true)
  }
}

object Documenter {

  def document[A <: GeneratedMessage](title: String, verb: String, desc: Node, request: A, responses: List[A]) = {
    <case>
      <title>{ title }</title>
      <desc>{ desc }</desc>
      <request verb={ verb }>
        { messageToXml(request) }
      </request>
      <response>
        { responses.map(messageToXml(_)) }
      </response>
    </case>
  }

  def getContent(obj: Any): Any = obj match {
    case list: java.util.List[_] => listField(list.toList)
    case subMessage: GeneratedMessage => messageToXml(subMessage)
    case enum: EnumValueDescriptor => enum.getName
    case x: Any => x.toString
  }

  def listField(list: List[_]) = {
    val entries = list.map { obj =>
      <entry>{ getContent(obj) }</entry>
    }
    NodeSeq.fromSeq(entries)
  }

  def messageToXml(msg: GeneratedMessage): NodeSeq = {

    val fields = msg.getAllFields.toList.map {
      case (desc, obj) =>
        <field name={ desc.getName }>{ getContent(obj) }</field>
    }

    <message className={ msg.getDescriptorForType.getName }>
      { NodeSeq.fromSeq(fields) }
    </message>
  }
}