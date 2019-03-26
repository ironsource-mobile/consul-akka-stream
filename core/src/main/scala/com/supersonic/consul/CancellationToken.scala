package com.supersonic.consul

import scala.concurrent.Promise

/** A token to signal cancellation between different parties. */
trait CancellationToken {
  def cancel(): Unit

  def isCancelled(): Boolean
}

class PromiseCancellationToken extends CancellationToken {
  val promise = Promise[Unit]()

  def cancel() = {
    val _ = promise.trySuccess(())
  }

  def isCancelled() = promise.isCompleted
}
