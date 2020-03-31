package com.ovoenergy.akka

import akka.actor.{ActorSystem, ExtendedActorSystem}
import com.ovoenergy.ConfigFixture
import org.scalatest.{BeforeAndAfterEach, Suite}

trait AkkaFixture extends BeforeAndAfterEach { self: Suite with ConfigFixture =>

  private var _system: ActorSystem = _

  implicit def system: ActorSystem =
    Option(_system).getOrElse(throw new IllegalStateException("ActorSystem not yet started"))
  def extendedActorSystem: ExtendedActorSystem = system match {
    case eas: ExtendedActorSystem => eas
    case _                        => throw new IllegalStateException("ActorSystem not an instance of ExtendedActorSystem")
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    _system = ActorSystem(actorSystemNameFrom(getClass), config)
  }

  override protected def afterEach(): Unit = {
    system.terminate()
    super.afterEach()
  }

  private def actorSystemNameFrom(clazz: Class[_]) =
    clazz.getName
      .replace('.', '-')
      .replace('_', '-')
      .filter(_ != '$')
}
