package com.ovoenergy.akka

import java.nio.file.Files

import akka.actor._
import akka.pattern._
import akka.persistence.{DeleteMessagesSuccess, PersistentActor}
import com.ovoenergy.ConfigFixture
import com.ovoenergy.akka.AkkaPersistenceFixture.JournalWriter.DeleteFromJournal
import com.ovoenergy.akka.AkkaPersistenceFixture._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration._
import scala.reflect.ClassTag

object AkkaPersistenceFixture {

  object JournalWriter {

    case class DeleteFromJournal(toSequenceNr: Long)

    def props(persistenceId: String): Props = Props(new JournalWriter(persistenceId))
  }

  class JournalWriter(val persistenceId: String) extends PersistentActor {

    // scalastyle:off var.field
    // It is very naive, does not support multiple concurrent deletions
    private var waitForDeletion = Option.empty[ActorRef]
    // scalastyle:on var.field

    override def receiveRecover: Receive =
      Actor.emptyBehavior

    override def receiveCommand: Receive = {

      case DeleteFromJournal(toSequenceNr) =>
        deleteMessages(toSequenceNr)
        waitForDeletion = Some(sender())

      case msg: DeleteMessagesSuccess =>
        waitForDeletion.foreach(_ ! msg)
        waitForDeletion = None

      case event =>
        persist(event) { persisted => sender() ! persisted }
    }
  }

}

trait AkkaPersistenceFixture extends ConfigFixture with ScalaFutures with BeforeAndAfterEach with Eventually {
  self: Suite with AkkaFixture with Notifying =>

  override protected def initConfig(): Config = {

    val journalDir  = Files.createTempDirectory("journal")
    val snapshotDir = Files.createTempDirectory("snapshot")

    note(s"Journal dir: $journalDir Snapshot dir: $snapshotDir")

    ConfigFactory.parseString(s"""
         |akka.persistence.journal.leveldb.dir = "${journalDir.toAbsolutePath.toString}"
         |akka.persistence.snapshot-store.local.dir = "${snapshotDir.toAbsolutePath.toString}"
      """.stripMargin).withFallback(super.initConfig())
  }

  def withJournalWriter[T](pId: String)(f: ActorRef => T): T = {

    val writer = system.actorOf(JournalWriter.props(pId))
    try {
      f(writer)
    } finally {
      writer ! PoisonPill
    }
  }

  def writeToJournal[T](persistenceId: String, event: T)(implicit tag: ClassTag[T]): T =
    withJournalWriter(persistenceId) { writer =>
      val written = writer.ask(event)(10.seconds).mapTo[T].futureValue(timeout(scaled(5.seconds)))
      note(s"Event written $written")
      written
    }

  def deleteFromJournal(persistenceId: String, toSequenceNr: Long): Unit = withJournalWriter(persistenceId) { writer =>
    writer
      .ask(DeleteFromJournal(toSequenceNr))(10.seconds)
      .mapTo[DeleteMessagesSuccess]
      .futureValue(timeout(scaled(5.seconds)))
    note(s"Events deleted from $persistenceId up to $toSequenceNr")
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventually(timeout(scaled(15.seconds)), interval(1.seconds)) {
      writeToJournal("test", "Test")
    }

    note("Journal initialized")
  }
}
