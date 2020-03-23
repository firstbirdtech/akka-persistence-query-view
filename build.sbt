
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
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
    // THe scaladoc is causing issue when generating doc around the snapshot format
    //publishArtifact in (Compile, packageDoc) := false,
    libraryDependencies ++= Dependencies.core

    //tutSettings,
    //tutTargetDirectory := baseDirectory.value,
    //bintrayOrganization := Some("firstbird"),
    //bintrayRepository := "maven",
    //bintrayPackageLabels := Seq("akka", "akka-persistence", "event-sourcing", "cqrs")
  )
  .enablePlugins(BuildInfoPlugin)
