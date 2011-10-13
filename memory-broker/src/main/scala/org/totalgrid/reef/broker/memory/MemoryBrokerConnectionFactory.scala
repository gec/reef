package org.totalgrid.reef.broker.memory

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.newapi.BrokerConnectionFactory

final class MemoryBrokerConnectionFactory(exe: Executor) extends BrokerConnectionFactory {

  import MemoryBrokerState._
  private var state = State()

  def update(fun: MemoryBrokerState.State => MemoryBrokerState.State) = synchronized(state = fun(state))
  def getState: MemoryBrokerState.State = state

  def connect = new MemoryBrokerConnection(this, exe)

}
