import sbt._
import sbt.Keys._
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

object Release {

  def setVersionOnly(selectVersion: Versions => String): ReleaseStep = { st: State =>
    val vs = st
      .get(ReleaseKeys.versions)
      .getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
    val selected = selectVersion(vs)

    st.log.info("Setting version to '%s'." format selected)
    val useGlobal = Project.extract(st).get(releaseUseGlobalVersion)

    reapply(
      Seq(
        if (useGlobal) version in ThisBuild := selected
        else version := selected
      ),
      st
    )
  }

  lazy val setReleaseVersionWithoutFile: ReleaseStep = setVersionOnly(_._1)
  lazy val setNextVersionWithoutFile: ReleaseStep = setVersionOnly(_._2)

}
