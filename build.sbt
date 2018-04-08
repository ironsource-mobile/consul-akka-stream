val akkaVersion = "2.5.7"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "consul-akka-stream",
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
      "-Ypartial-unification"
    ),
    libraryDependencies ++= (dependencies ++ testDependencies),
    sources in (Compile, doc) := List.empty
  )
  .settings(Defaults.itSettings: _*)
  .settings(
    parallelExecution in IntegrationTest := false,
    publishArtifact in (IntegrationTest, packageBin) := true,
    // see: https://github.com/sbt/sbt/issues/2458
    addArtifact(artifact in (IntegrationTest, packageBin), packageBin in IntegrationTest))

inThisBuild(List(
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/SupersonicAds/consul-akka-stream")),
  developers := List(Developer("SupersonicAds", "SupersonicAds", "SupersonicAds", url("https://github.com/SupersonicAds"))),
  scmInfo := Some(ScmInfo(url("https://github.com/SupersonicAds/consul-akka-stream"), "scm:git:git@github.com:SupersonicAds/consul-akka-stream.git")),

  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  releaseEarlyEnableSyncToMaven := false,
  releaseEarlyWith := BintrayPublisher
))

def dependencies = List(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
  "com.orbitz.consul" % "consul-client" % "1.0.1"
)

def testDependencies = List(
  "org.scalatest" %% "scalatest" % "3.0.4" % "it, test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "it, test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "it, test",
  "com.pszymczyk.consul" % "embedded-consul" % "1.0.1" % "it"
)
