package com.supersonic.consul

import java.math.BigInteger
import java.util.{List => JList}
import akka.event.LoggingAdapter
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.orbitz.consul.Consul
import com.orbitz.consul.async.ConsulResponseCallback
import com.orbitz.consul.model.ConsulResponse
import com.orbitz.consul.model.kv.Value
import com.orbitz.consul.option.QueryOptions
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object ConsulStream {

  /** Given a Consul key, creates a [[Source]] where a new entry is added upon each update of the
    * key (includes recursive update).
    *
    * Each entry contains the full set of keys that are contained under the key.
    */
  def consulKeySource(key: String,
                      consul: Consul,
                      blockingTime: FiniteDuration = 5.minutes)
                     (implicit executionContext: ExecutionContext,
                      logger: LoggingAdapter): Source[Map[String, Option[String]], CancellationToken] = {

    val zero = new BigInteger("0")

    // Adapted from https://github.com/akka/akka/issues/17769#issuecomment-206532674
    def peekMaterializedValue[A, M](src: Source[A, M]): (Source[A, CancellationToken], Future[M], CancellationToken) = {
      val p = Promise[M]
      val cancellationToken = new PromiseCancellationToken()
      val s = src.mapMaterializedValue { m =>
        p.trySuccess(m)
        cancellationToken
      }
      (s, p.future, cancellationToken)
    }

    val (source, eventualQueue, cancellationToken) = peekMaterializedValue {
      Source.queue[Map[String, Option[String]]](0, OverflowStrategy.backpressure)
    }

    eventualQueue.foreach { queue =>
      def watchWithCallback(callback: Callback) = {
        val query = QueryOptions.blockSeconds(blockingTime.toSeconds.toInt, callback.index).build()

        if (cancellationToken.isCancelled())
          queue.complete()
        else consul.keyValueClient.getValues(key, query, callback)
      }

      class Callback(val index: BigInteger) extends ConsulResponseCallback[JList[Value]] {
        def watch(index: BigInteger) = watchWithCallback(new Callback(index))

        def onComplete(consulResponse: ConsulResponse[JList[Value]]) = {
          def log(t: Throwable) = {
            logger.error(t, "Error when parsing a Consul value")

            Map.empty[String, Option[String]]
          }

          if (consulResponse.getIndex.compareTo(index) > 0) {
            val response = Option(consulResponse.getResponse) // the response can be null
              .map(_.asScala)
              .getOrElse(List.empty[Value])

            val values = Try {
              response.map { keyValue =>
                keyValue.getKey -> Option(keyValue.getValueAsString.orElse(null))
              }.toMap
            }.fold(log, identity)

            queue.offer(values)
          }

          watch(consulResponse.getIndex)
        }

        def onFailure(t: Throwable) = {
          logger.error(t, "Error while listening to Consul")
          watch(index)
        }
      }

      watchWithCallback(new Callback(zero))
    }

    source
  }
}
