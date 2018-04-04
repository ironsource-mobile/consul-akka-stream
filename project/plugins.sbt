logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0")

addSbtPlugin("ch.epfl.scala" % "sbt-release-early" % "2.1.1")