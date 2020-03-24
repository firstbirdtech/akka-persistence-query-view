
val homepageUrl = url("https://github.com/firstbirdtech/akka-persistence-query-view")

lazy val root = project
  .in(file("."))
  .settings(
    organization := "com.firstbird",
    organizationName := "Firstbird GmbH",
    organizationHomepage := Some(url("https://www.firstbird.com")),
    name := "akka-persistence-query-view",
    description := "An Akka PersistentView replacement",
    homepage := Some(homepageUrl),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    developers += Developer(
      "contributors",
      "Contributors",
      "hello@firstbird.com",
      homepageUrl
    ),
    scalaVersion := "2.13.1",
    crossScalaVersions := Seq(scalaVersion.value, "2.12.11"),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Xlint",
      "-Ywarn-dead-code",
    ),
    javacOptions ++= Seq(
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    ),
    libraryDependencies ++= Dependencies.core

    //tutSettings,
    //tutTargetDirectory := baseDirectory.value,
  )
  .enablePlugins(BuildInfoPlugin)
