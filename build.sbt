name := "consul-akka-stream-root"

lazy val core =
  project
    .settings(name := "consul-akka-stream")
    .settings(baseSettings: _*)
    .settings(libraryDependencies ++= dependencies)
    .settings(
      parallelExecution in IntegrationTest := false)

// this is separate project because we want to publish artifacts from it
lazy val `integration-tests` =
  project
    .settings(name := "consul-akka-stream-integration-tests")
    .settings(baseSettings: _*)
    .settings(libraryDependencies ++= testDependencies)
    .dependsOn(core)

def baseSettings = List(
  organization := "com.supersonic",
  scalaVersion := "2.12.5",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Xlint",
    "-Ypartial-unification"),
  sources in (Compile, doc) := List.empty)

inThisBuild(List(
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/SupersonicAds/consul-akka-stream")),
  developers := List(Developer("SupersonicAds", "SupersonicAds", "SupersonicAds", url("https://github.com/SupersonicAds"))),
  scmInfo := Some(ScmInfo(url("https://github.com/SupersonicAds/consul-akka-stream"), "scm:git:git@github.com:SupersonicAds/consul-akka-stream.git")),

  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  releaseEarlyEnableSyncToMaven := false,
  releaseEarlyWith := BintrayPublisher))

val akkaVersion = "2.5.7"

def dependencies = List(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
  "com.orbitz.consul" % "consul-client" % "1.0.1")

def testDependencies = List(
  // These are not marked with '% "test"' because we are publishing an artifact with them
  "org.scalatest" %% "scalatest" % "3.0.4",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.pszymczyk.consul" % "embedded-consul" % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3")
