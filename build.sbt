import Dependencies._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._

lazy val `akka-persistence-query-view` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    organization := "com.github.filosganga",
    name := "akka-persistence-query-view",
    startYear := Some(2016),

    version := "1.0-SNAPSHOT",

    git.remoteRepo := "origin",
    git.runner := ConsoleGitRunner,
    git.baseVersion := "1.0",
    git.useGitDescribe := true,

    licenses := Seq(
      ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
    ),

    scalaVersion := "2.11.8",

    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.typesafeRepo("releases")
    ),

    libraryDependencies ++= Seq(
      typesafe.config,
      slf4j.api,
      // -- Akka
      akka.actor,
      akka.slf4j,
      akka.stream,
      akka.persistence,
      akka.persistenceQuery,
      // -- Testing --
      scalaTest % Test,
      scalaCheck % Test,
      scalaMock.scalaTestSupport % Test,
      akka.streamTestKit % Test,
      logback.classic % Test
    )
  )