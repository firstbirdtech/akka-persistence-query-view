package com.ovoenergy

import org.scalatest.concurrent.{ScalaFutures, ScaledTimeSpans}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

abstract class UnitSpec
    extends WordSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with ScalaFutures
    with ScaledTimeSpans
    with ConfigFixture {

  // AbstractPatienceConfiguration define a class that call span
  override def spanScaleFactor: Double = optionalConfig.getOrElse(initConfig()).getDouble("akka.test.timefactor")
}
