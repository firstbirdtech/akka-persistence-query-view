import sbt._

object Dependencies {

  object typesafe {
    val config = "com.typesafe" % "config" % "1.3.0"
  }

  object slf4j {

    private val version = "1.7.12"

    val api = "org.slf4j" % "slf4j-api" % version
    val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % version
    val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % version
  }

  object log4j {
    private val version = "2.5"

    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % version
  }

  object logback {

    private val version = "1.1.3"

    val core = "ch.qos.logback" % "logback-core" % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object akka {

    private val version = "2.4.14"

    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val remote = "com.typesafe.akka" %% "akka-remote" % version
    val cluster = "com.typesafe.akka" %% "akka-cluster" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    val contrib = "com.typesafe.akka" %% "akka-contrib" % version
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % version
    val clusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % version
    val clusterMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % version
    val persistence = "com.typesafe.akka" %% "akka-persistence" % version
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query-experimental" % version
    val persistenceTck = "com.typesafe.akka" %% "akka-persistence-tck" % version
    val testKit = "com.typesafe.akka" %% "akka-testkit" % version
    val stream = "com.typesafe.akka" %% "akka-stream" % version
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % version
  }

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.5"

  object scalaMock {

    private val version = "3.2"

    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % version
  }

}