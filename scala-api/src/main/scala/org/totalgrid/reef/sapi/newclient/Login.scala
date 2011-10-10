package org.totalgrid.reef.sapi.newclient

import net.agileautomata.executor4s._

trait Login {

  def login(authToken: String): Client
  def login(userName: String,  password: String): Future[Result[Client]]

}