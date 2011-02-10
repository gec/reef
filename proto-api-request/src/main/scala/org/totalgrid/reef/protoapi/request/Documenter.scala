package org.totalgrid.reef.protoapi.request

import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConversions._
import com.google.protobuf.Descriptors.EnumValueDescriptor
import xml.{XML, NodeSeq}

class Documenter(file: String, title: String, desc: String) {

  protected var usages = List.empty[NodeSeq]

  def addCase[A <: GeneratedMessage](title: String, desc: String, request: A, response: A) = {
    usages ::= Documenter.document(title, desc, request, response)
  }

  def save = {
    val content =
      <servicedoc>
        <title>{title}</title>
        <desc>{desc}</desc>
        {usages.reverse}
      </servicedoc>

    XML.save(/*"servicedoc/" +*/ file, content, "UTF-8", true)
  }
}

object Documenter {

  def document[A <: GeneratedMessage](title: String, desc:String, request: A, response: A) = {
    <case>
      <title>{title}</title>
      <desc>{desc}</desc>
      <request>
        {messageToXml(request)}
      </request>
      <response>
        {messageToXml(response)}
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
      <entry>{getContent(obj)}</entry>
    }
    NodeSeq.fromSeq(entries)
  }

  def messageToXml(msg: GeneratedMessage): NodeSeq = {

    val fields = msg.getAllFields.toList.map {
      case (desc, obj) =>
        <field name={desc.getName}>{getContent(obj)}</field>
    }

    <message className={msg.getDescriptorForType.getName}>
      {NodeSeq.fromSeq(fields)}
    </message>
  }
}