import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  object akka {
    private val version = "2.5.13"

    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val remote = "com.typesafe.akka" %% "akka-remote" % version
    val cluster = "com.typesafe.akka" %% "akka-cluster" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    val contrib = "com.typesafe.akka" %% "akka-contrib" % version
    val clusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % version
    val clusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % version
    val clusterMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % version
    val persistence = "com.typesafe.akka" %% "akka-persistence" % version
    val protobuf = "com.typesafe.akka" %% "akka-protobuf" % version
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % version
    val persistenceTck = "com.typesafe.akka" %% "akka-persistence-tck" % version
    val testKit = "com.typesafe.akka" %% "akka-testkit" % version
    val stream = "com.typesafe.akka" %% "akka-stream" % version
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % version
  }

  object log4j {
    private val version = "2.7"

    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % version
  }

  object logback {
    private val version = "1.1.8"

    val core = "ch.qos.logback" % "logback-core" % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object slf4j {
    private val version = "1.7.25"

    val api = "org.slf4j" % "slf4j-api" % version
    val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % version
    val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % version
    val nop = "org.slf4j" % "slf4j-nop" % version
  }

  object typesafe {
    private val version = "1.3.3"

    val config = "com.typesafe" % "config" % version
  }

  // TODO: Are those needed?
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.5"

  object LevelDb {
    val levelDb = "org.iq80.leveldb" % "leveldb" % "0.10"
    val leveldbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  }

  object scalaMock {
    private val version = "3.6.0"

    val scalaTestSupport = "org.scalamock" %% "scalamock-scalatest-support" % version
  }

  val core: Seq[ModuleID] = Seq(
    akka.actor,
    akka.stream,
    akka.persistence,
    akka.persistenceQuery,
    akka.protobuf,
    slf4j.api,
    typesafe.config,
    // -- Testing --
    scalaTest % Test,
    scalaCheck % Test,
    akka.streamTestKit % Test,
    akka.slf4j % Test,
    logback.classic % Test,
    scalaMock.scalaTestSupport % Test,
    LevelDb.levelDb % Test
  )
}
