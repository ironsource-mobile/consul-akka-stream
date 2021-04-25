import sbtghactions.RefPredicate.Equals

name := "consul-akka-stream-root"
// no need to publish the root project
publishArtifact := false

lazy val core =
  project
    .settings(name := "consul-akka-stream")
    .settings(baseSettings: _*)
    .settings(libraryDependencies ++= dependencies)

// this is separate project because we want to publish artifacts from it
lazy val `integration-tests` =
  project
    .settings(name := "consul-akka-stream-integration-tests")
    .settings(baseSettings: _*)
    .settings(libraryDependencies ++= testDependencies)
    .dependsOn(core)

def baseSettings = List(
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
  sonatypeCredentialHost := Sonatype.sonatype01,
  Compile / doc / sources := List.empty)

sonatypeCredentialHost := Sonatype.sonatype01
inThisBuild(List(
  organization := "com.supersonic",
  homepage := Some(url("https://github.com/SupersonicAds/consul-akka-stream")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(Developer("SupersonicAds", "SupersonicAds", "SupersonicAds", url("https://github.com/SupersonicAds"))),

  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}")))))

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
