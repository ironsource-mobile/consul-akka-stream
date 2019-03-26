[![Build Status](https://travis-ci.org/SupersonicAds/consul-akka-stream.svg?branch=master)](https://travis-ci.org/SupersonicAds/consul-akka-stream) [![Download](https://api.bintray.com/packages/ironsonic/maven/consul-akka-stream/images/download.svg) ](https://bintray.com/ironsonic/maven/consul-akka-stream/_latestVersion)

# consul-akka-stream

`consul-akka-stream` is a small utility that can listen to a [Consul](https://www.consul.io/) key in its key/value store and provide an [Akka stream](https://doc.akka.io/docs/akka/current/stream/index.html) that emits a value every time the key is updated.

## Getting consul-akka-stream

Add these lines you to your SBT project:
```scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "com.supersonic" %% "consul-akka-stream" % "1.0.4"
```

## How To

To create a stream we first need a Consul client, we use the [Consul Client for Java](https://github.com/rickfast/consul-client) library:
```scala
val consul =
  Consul.builder()
    .withReadTimeoutMillis(0L)
    .withHostAndPort(HostAndPort.fromParts("localhost", 8500))
    .build()
```

With this in hand, we can initialize an Akka-streams source based on the client using the `ConsulStream.consulKeySource` function:

```scala
val source: Source[Map[String, Option[String]], CancellationToken] = 
  ConsulStream.consulKeySource(key = "foo/baz/bar", consul, blockingTime = 1.seconds)
```

This creates an Akka source that listens to the `foo/baz/bar` key in Consul's key/value store. On every change to the key (including recursive changes) a new map is produced. The keys in the map are the Consul keys and the values are the (optional) strings under them. 

An example output can be something like:
```scala
Map(
  "foo/baz/bar/qux" -> Some("a"), 
  "foo/baz/bar/goo" -> Some("b"), 
  "foo/baz/bar/bla" -> None)
```

The Source materializes into a `CancellationToken` token that can be triggered when the user wants to stop polling Consul.

See the [tests](https://github.com/SupersonicAds/consul-akka-stream/blob/master/src/it/scala/com/supersonic/consul/ConsulStreamTest.scala) for further examples.

## Integration Tests

The library provides a trait that facilitates testing the Consul stream (with ScalaTest) using the [Embedded Consul](https://github.com/pszymczyk/embedded-consul) library.

To obtain the trait, add the following to your SBT project:
```scala
libraryDependencies += "com.supersonic" %% "consul-akka-stream" % "1.0.4" % "it" classifier "it"
```

And mixin the [`ConsulIntegrationSpec`](https://github.com/SupersonicAds/consul-akka-stream/blob/master/src/it/scala/com/supersonic/consul/ConsulIntegrationSpec.scala) trait into your test.
