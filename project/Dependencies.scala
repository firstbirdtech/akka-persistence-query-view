import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  object akka {
    private val version = "2.5.30"

    val actor            = "com.typesafe.akka" %% "akka-actor"             % version
    val slf4j            = "com.typesafe.akka" %% "akka-slf4j"             % version
    val persistence      = "com.typesafe.akka" %% "akka-persistence"       % version
    val protobuf         = "com.typesafe.akka" %% "akka-protobuf"          % version
    val persistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % version
    val stream           = "com.typesafe.akka" %% "akka-stream"            % version
    val streamTestKit    = "com.typesafe.akka" %% "akka-stream-testkit"    % version
  }

  object logback {
    val classic = "ch.qos.logback" % "logback-classic" % "1.1.8"
  }

  object slf4j {
    val api = "org.slf4j" % "slf4j-api" % "1.7.30"
  }

  object typesafe {
    val config = "com.typesafe" % "config" % "1.4.0"
  }

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.0.8"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3"

  object levelDb {
    val levelDb = "org.iq80.leveldb" % "leveldb" % "0.12"
  }

  object scalaMock {
    val scalaTestSupport = "org.scalamock" %% "scalamock" % "4.4.0"
  }

  object scala {
    val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"
  }

  val core: Seq[ModuleID] = Seq(
    akka.actor,
    akka.stream,
    akka.persistence,
    akka.persistenceQuery,
    akka.protobuf,
    logback.classic,
    slf4j.api,
    typesafe.config,
    // -- Testing --
    scalaTest                  % Test,
    scalaCheck                 % Test,
    akka.slf4j                 % Test,
    akka.streamTestKit         % Test,
    levelDb.levelDb            % Test,
    scalaMock.scalaTestSupport % Test,
    // -- Backwards Compatibility --
    scala.collectionCompat
  )
}
