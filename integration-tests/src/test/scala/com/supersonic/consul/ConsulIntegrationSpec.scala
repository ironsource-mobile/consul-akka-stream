package com.supersonic.consul

import akka.event.LoggingAdapter
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import com.google.common.net.HostAndPort
import com.orbitz.consul.Consul
import com.pszymczyk.consul.{ConsulProcess, ConsulStarterBuilder}
import org.scalatest.{BeforeAndAfterAll, TestSuite}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** A base trait for tests that need an embedded Consul instance. */
trait ConsulIntegrationSpec extends TestSuite with BeforeAndAfterAll {
  self: TestKit =>

  @volatile var consulProcess: ConsulProcess = _

  lazy val consul = {
    val host = HostAndPort.fromParts("localhost", consulProcess.getHttpPort)
    Consul.builder()
      .withReadTimeoutMillis(0L) // no timeout
      .withHostAndPort(host)
      .build()
  }

  def keyValueClient = consul.keyValueClient

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    consulProcess = ConsulStarterBuilder.consulStarter().build().start()
  }

  override def afterAll(): Unit = {
    consulProcess.close()
    super.afterAll()
  }

  /** Runs a a test with a probe against a Consul stream, making sure to cancel the stream
    * after the test.
    */
  def withConsulProbe[A](key: String,
                         consul: Consul,
                         blockingTime: FiniteDuration = 10.seconds)
                        (f: Probe[Map[String, Option[String]]] => A)
                        (implicit executionContext: ExecutionContext,
                         logger: LoggingAdapter,
                         materializer: Materializer): A =
    withStreamFromConsulProbe(key, consul, blockingTime)(identity)(f)


  /** Runs a a test with a probe against a stream derived from a Consul stream,
    * making sure to cancel the stream after the test.
    */
  def withStreamFromConsulProbe[A, B](key: String,
                                      consul: Consul,
                                      blockingTime: FiniteDuration = 10.seconds)
                                     (transform: Source[Map[String, Option[String]], CancellationToken] => Source[A, CancellationToken])
                                     (f: Probe[A] => B)
                                     (implicit executionContext: ExecutionContext,
                                      logger: LoggingAdapter,
                                      materializer: Materializer): B = {
    val source = transform(ConsulStream.consulKeySource(key, consul, blockingTime))
    val (cancellationToken, probe) = source.toMat(TestSink.probe)(Keep.both).run()

    try f(probe)
    finally cancellationToken.cancel()
  }
}
