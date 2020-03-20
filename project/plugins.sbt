addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0") // Where is this even used?
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.4.0") // Is this worth anything?
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
//addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.4.8") // Deprecated -> use mdoc instead

// To resolve custom bintray-sbt plugin
resolvers += Resolver.url("2m-sbt-plugin-releases", url("https://dl.bintray.com/2m/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns
)

// TO mute sbt-git
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.22"
