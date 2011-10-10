package org.totalgrid.reef.sapi.newclient

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.sapi.client.DefaultHeaders

trait Client extends Executor with RestOperations with Subscribable with DefaultHeaders