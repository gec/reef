package org.totalgrid.reef.api.sapi.client.impl

import org.totalgrid.reef.api.sapi.client.Promise
import net.agileautomata.executor4s.Result

class FixedPromise[A](result: Result[A]) extends Promise[A] {
  def await: A = result.get
  def listen(fun: Promise[A] => Unit): Promise[A] = {
    fun(this)
    this
  }
  def extract: Result[A] = result
  def map[B](fun: A => B): Promise[B] = new FixedPromise[B](result.map(fun))
  def isComplete: Boolean = true
}