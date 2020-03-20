import Dependencies._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.git._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

lazy val `akka-persistence-query-view` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(
    organization := "com.firstbird",
    organizationHomepage := Some(url("https://www.firstbird.com")),
    description := "An Akka PersistentView replacement",
    name := "akka-persistence-query-view",
    homepage := Some(url("https://github.com/firstbirdtech/akka-persistence-query-view")),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    developers := List(
      Developer(
        "pedro.dias@firstbird.com",
        "Pedro Dias",
        "pedro.dias@firstbird.com",
        url("https://firstbird.com")
      )
    ),
    git.remoteRepo := "origin",
    git.runner := ConsoleGitRunner,
    git.baseVersion := "0.1.0",
    git.useGitDescribe := true,
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
    resolvers ++= Seq(Resolver.mavenLocal, Resolver.typesafeRepo("releases")),
    // THe scaladoc is causing issue when generating doc around the snapshot format
    publishArtifact in (Compile, packageDoc) := false,
    libraryDependencies ++= Seq(
      typesafe.config,
      slf4j.api,
      // -- Akka
      akka.actor,
      akka.stream,
      akka.persistence,
      akka.persistenceQuery,
      akka.protobuf,
      // -- Testing --
      scalaTest % Test,
      scalaCheck % Test,
      scalaMock.scalaTestSupport % Test,
      akka.streamTestKit % Test,
      akka.slf4j % Test,
      logback.classic % Test,
      LevelDb.levelDb % Test
    ),
    //tutSettings,
    //tutTargetDirectory := baseDirectory.value,
    bintrayOrganization := Some("firstbird"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("akka", "akka-persistence", "event-sourcing", "cqrs")
  )
