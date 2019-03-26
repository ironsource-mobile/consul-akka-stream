package com.supersonic.consul

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest.{Matchers, WordSpecLike}
import scala.concurrent.duration._

class ConsulStreamTest extends TestKit(ActorSystem("ConsulStreamTest"))
                               with ConsulIntegrationSpec
                               with WordSpecLike
                               with Matchers {
  implicit val materializer = ActorMaterializer()(system)
  implicit val ec = system.dispatcher
  implicit val logger = Logging(system.eventStream, "ConsulStreamTest")

  "The Consul stream" should {
    "listen to changes under a key and the current state of the key" in {
      keyValueClient.putValue("foo/bar", "a")
      keyValueClient.putValue("foo/qux", "b")
      keyValueClient.putValue("foo/baz")
      keyValueClient.putValue("foo/baz/qux", "c")

      withConsulProbe("foo", consul) { probe =>
        probe.requestNext() shouldBe
          Map("foo/bar" -> Some("a"), "foo/qux" -> Some("b"), "foo/baz" -> None, "foo/baz/qux" -> Some("c"))

        keyValueClient.putValue("foo/qux", "d")

        probe.requestNext() shouldBe
          Map("foo/bar" -> Some("a"), "foo/qux" -> Some("d"), "foo/baz" -> None, "foo/baz/qux" -> Some("c"))
      }
    }

    "fetch an empty map when the key is not populated" in {
      withConsulProbe("bar", consul) { probe =>
        probe.requestNext() shouldBe empty

        consul.keyValueClient.putValue("bar")
        probe.requestNext() shouldBe Map("bar" -> None)
      }
    }

    "not send new messages when there are no updates" in {
      keyValueClient.putValue("goo")

      withConsulProbe("goo", consul, blockingTime = 1.seconds) { probe =>
        probe.requestNext() shouldBe Map("goo" -> None)
        probe.request(1)
        probe.expectNoMessage(3.seconds)
      }
    }

    "stop listening to Consul when cancelled" in {
      keyValueClient.putValue("baz/not-cancelled")

      val source = ConsulStream.consulKeySource("baz", consul, blockingTime = 1.seconds)

      val (cancellationToken, probe) = source.toMat(TestSink.probe)(Keep.both).run()

      probe.requestNext() shouldBe Map("baz/not-cancelled" -> None)

      cancellationToken.cancel()
      probe.request(5)
      probe.expectComplete()

      keyValueClient.putValue("baz/cancelled")
      probe.expectNoMessage(2.seconds)
    }
  }
}
