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
package org.totalgrid.reef.protoapi.request

import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConversions._
import com.google.protobuf.Descriptors.EnumValueDescriptor
import xml.{ XML, NodeSeq }
import java.io.File

class Documenter(file: String, title: String, desc: String) {

  protected var usages = List.empty[NodeSeq]

  def addCase[A <: GeneratedMessage](title: String, desc: String, request: A, response: A) = {
    usages ::= Documenter.document(title, desc, request, response)
  }

  def save = {
    val content =
      <servicedoc>
        <title>{ title }</title>
        <desc>{ desc }</desc>
        { usages.reverse }
      </servicedoc>

    val path = "target/docxml"
    val dir = new File(path)
    dir.mkdirs

    XML.save(path + "/" + file, content, "UTF-8", true)
  }
}

object Documenter {

  def document[A <: GeneratedMessage](title: String, desc: String, request: A, response: A) = {
    <case>
      <title>{ title }</title>
      <desc>{ desc }</desc>
      <request>
        { messageToXml(request) }
      </request>
      <response>
        { messageToXml(response) }
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