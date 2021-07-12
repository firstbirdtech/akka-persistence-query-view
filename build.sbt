ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

addCommandAlias("codeFmt", ";scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias("codeVerify", ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check")

lazy val commonSettings = Seq(
  organization := "com.firstbird",
  organizationName := "Firstbird GmbH",
  sonatypeProfileName := "com.firstbird",
  homepage := Some(url("https://github.com/firstbirdtech/akka-persistence-query-view")),
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(homepage.value.get, "scm:git:https://github.com/firstbirdtech/akka-persistence-query-view.git")
  ),
  developers += Developer(
    "contributors",
    "Contributors",
    "hello@firstbird.com",
    url("https://github.com/firstbirdtech/backbone/graphs/contributors")
  ),
  startYear := Some(2016),
  scalaVersion := "2.13.3",
  crossScalaVersions := Seq("2.12.14", scalaVersion.value),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-language:higherKinds",
    "-unchecked",
    "-Xcheckinit",
    "-Xfatal-warnings"
  ),
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wdead-code", "-Wunused:imports")
      case _             => Seq("-Xfuture", "-Ywarn-dead-code", "-Ywarn-unused:imports", "-Yno-adapted-args")
    }
  ),
  javacOptions ++= Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  // show full stack traces and test case durations
  testOptions in Test += Tests.Argument("-oDF"),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "akka-persistence-query-view",
    libraryDependencies ++= Dependencies.core,
    // Needed because the API doc for Akka-Persistence can't be found and the warning would abort publishing
    Compile / doc / scalacOptions := Seq()
  )
