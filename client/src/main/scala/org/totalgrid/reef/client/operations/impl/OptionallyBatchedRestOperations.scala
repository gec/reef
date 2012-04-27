package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.operations.RestOperations

trait OptionallyBatchedRestOperations extends RestOperations {
  def batched: Option[BatchRestOperations]
}

/*
sealed trait BatchOptionalOperations {
  def operations: RestOperations
  def batched: Option[BatchRestOperations]
}


case class NotBatched(operations: RestOperations) extends BatchOptionalOperations {
  def batched: Option[BatchRestOperations] = None
}

case class Batched(operations: BatchRestOperations) extends BatchOptionalOperations {
  def batched: Option[BatchRestOperations] = Some(operations)
}*/
