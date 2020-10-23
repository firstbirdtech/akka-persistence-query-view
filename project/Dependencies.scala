import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  private val akkaVersion = "2.5.31"

  val core: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor"             % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence"       % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf"          % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"            % akkaVersion,
    "ch.qos.logback"     % "logback-classic"        % "1.1.8",
    "org.slf4j"          % "slf4j-api"              % "1.7.30",
    "com.typesafe"       % "config"                 % "1.4.1",
    // -- Testing --
    "org.scalatest"     %% "scalatest"           % "3.2.0"     % Test,
    "org.scalatestplus" %% "scalacheck-1-14"     % "3.2.0.0"   % Test,
    "com.typesafe.akka" %% "akka-slf4j"          % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.iq80.leveldb"   % "leveldb"             % "0.12"      % Test,
    // -- Backwards Compatibility --
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"
  )
}
