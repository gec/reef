package org.totalgrid.reef.protoapi.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.{CommandRequest, UserCommandRequest}

object Commands extends CommandRequestBuilders

object CommandRequestTestDesc {
  def desc =
    "Clients use put to issue a command. The CommandRequest object describes the command " +
    "to be executed, and timeout can be specified by the client code. " +
    "Status and user are not specified in put. User is identified from the request header."
}

@RunWith(classOf[JUnitRunner])
class CommandRequestTest
  extends ServiceClientSuite("usercommandrequest.xml", "UserCommandRequest", CommandRequestTestDesc.desc)
  with ShouldMatchers {


  test ("Issue command") {
    val desc = "Issue a command request for the specified point."

    val cmdName = "StaticSubstation.Breaker02.Trip"
    val acc = Commands.allowAccessForCommand(cmdName)
    val accResp = client.putOneOrThrow(acc)

    val req = Commands.executeCommand(cmdName)
    val resp = client.putOneOrThrow(req)

    doc.addCase("Issue command", desc, req, resp)

  }

}