resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//use patched versions by now, to make scoverage work with scalajs-bundler
addSbtPlugin(("org.scommons.patched" % "sbt-scalajs-bundler" % "0.14.0-SNAPSHOT").force())

addSbtPlugin("org.scommons.sbt" % "sbt-scommons-plugin" % "0.3.0")
