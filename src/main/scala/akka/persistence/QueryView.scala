package akka.persistence

import akka.actor._
import akka.pattern._
import akka.persistence.SnapshotProtocol.LoadSnapshotResult
import akka.persistence.query.{EventEnvelope, EventEnvelope2, Offset, Sequence}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.github.filosganga.akka.persistence.QuerySupport

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.control.NonFatal

object QueryView {

  val DefaultRecoveryTimeout: Duration = 120.seconds

  val DefaultLoadSnapshotTimeout: Duration = 5.seconds

  case class RecoveringStatus[T](data: T, maxOffset: Offset, sequenceNrs: Map[String, Long] = Map.empty)

  case class LoadSnapshotFailed(cause: Throwable)

  case object RecoveryCompleted

  case class RecoveryFailed(cause: Throwable)

  case class LiveStreamFailed(cause: Throwable)

  case object LiveStreamCompleted extends Exception("The live events stream complete (It should never)")

  sealed trait State

  object State {

    case object WaitingForSnapshot extends State

    case object Recovering extends State

    case object Live extends State

  }

}

abstract class QueryView extends Actor
  with Snapshotter
  with QuerySupport
  with Stash
  with StashFactory
  with ActorLogging {

  import QueryView._
  import context._

  // Status variables

  private[this] var _lastOffset: Offset = firstOffset
  private[this] var sequenceNrByPersistenceId: Map[String, Long] = Map.empty
  private[this] var lastSnapshotSequenceNr: Long = 0L
  private[this] var _noOfEventsSinceLastSnapshot: Long = 0L
  private[this] var _isSavingSnapshot: Boolean = false
  private[this] var recoveryStartedAt: Long = 0L
  private[this] var currentState: State = State.WaitingForSnapshot
  private[this] var loadSnapshotTimer: Option[Cancellable] = None

  private val persistence = Persistence(context.system)
  override private[persistence] val snapshotStore: ActorRef = persistence.snapshotStoreFor(snapshotPluginId)
  private implicit val materializer = ActorMaterializer()(context)
  private val recoveringStash = createStash()

  /**
    * It is the persistenceId linked to this view. It should be unique.
    */
  override def snapshotterId: String

  /**
    * Configuration id of the snapshot plugin servicing this persistent actor or view.
    * When empty, looks in `akka.persistence.snapshot-store.plugin` to find configuration entry path.
    * When configured, uses `snapshotPluginId` as absolute path to the snapshot store configuration entry.
    * Configuration entry must contain few required fields, such as `class`. See akka-persistence jar
    * `src/main/resources/reference.conf`.
    */
  def snapshotPluginId: String = ""

  /**
    * The amount of time this actor must wait until giving up waiting for the recovery process. A undefined duration
    * causes the actor to wait indefinitely. If the recovery fails because of a timeout, this actor will crash.
    *
    * TODO we can tune by a flag to indicate we want the actor to switch live if the recovery timeout.
    */
  def recoveryTimeout: Duration = DefaultRecoveryTimeout

  /**
    * The amount of time this actor must wait until giving up waiting for a snapshot loading. A undefined duration
    * causes the actor to wait indefinitely. The timeout does not cause this actor to crash, it is a recoverable error.
    */
  def loadSnapshotTimeout: Duration = DefaultLoadSnapshotTimeout

  /**
    * It is the source od EventEnvelope used to recover the view status. It MUST be finite stream.
    */
  def recoveringStream(): Source[EventEnvelope, _]

  /**
    * It is the source od EventEnvelope used to receive live events, it MUST be a infinite stream (eg: It should never
    * complete)
    */
  def liveStream(): Source[EventEnvelope, _]

  /**
    * It is an hook called before the actor switch to live mode. It is synchronous (it can change the actor status).
    */
  def preLive(): Unit = {}

  // Status accessors

  final def isWaitingForSnapshot: Boolean = currentState == State.WaitingForSnapshot

  final def isRecovering: Boolean = currentState == State.Recovering

  final def isLive: Boolean = currentState == State.Live

  final def lastOffset: Offset = _lastOffset

  final def nextSequenceNr(persistenceId: String): Long = sequenceNrByPersistenceId.getOrElse(persistenceId, 0L)

  /**
    * This will return the number of processed events since last snapshot has been taken.
    */
  final def noOfEventSinceLastSnapshot(): Long = _noOfEventsSinceLastSnapshot

  final def isSavingSnapshot: Boolean = _isSavingSnapshot

  override final def snapshotSequenceNr: Long = lastSnapshotSequenceNr + 1

  // Behavior

  override protected[akka] def aroundPreStart(): Unit = {
    super.aroundPreStart()
    loadSnapshot(snapshotterId, SnapshotSelectionCriteria.Latest, Long.MaxValue)
    // If the `loadSnapshotTimeout` is finite, it makes sure the Actor will not get stuck in 'waitingForSnapshot' state.
    loadSnapshotTimer = loadSnapshotTimeout match {
      case timeout: FiniteDuration ⇒
        Some(context.system.scheduler.scheduleOnce(timeout, self, LoadSnapshotFailed(new TimeoutException(s"Load snapshot timeout after $timeout"))))
      case _ ⇒
        None
    }
    recoveryStartedAt = System.currentTimeMillis()
    currentState = State.WaitingForSnapshot
  }

  override protected[akka] def aroundPostStop(): Unit = {
    loadSnapshotTimer.foreach(_.cancel())
    materializer.shutdown()
    super.aroundPostStop()
  }

  override protected[akka] def aroundReceive(behaviour: Receive, msg: Any): Unit = {

    if (isWaitingForSnapshot) {
      waitingForSnapshot(behaviour, msg)
    } else if (isRecovering) {
      recovering(behaviour, msg)
    } else {
      assert(isLive)
      live(behaviour, msg)
    }
  }

  private def live(behaviour: Receive, msg: Any) = {
    msg match {
      case EventEnvelope2(offset, persistenceId, sequenceNr, event) ⇒
        _lastOffset = offset
        sequenceNrByPersistenceId = sequenceNrByPersistenceId + (persistenceId -> (sequenceNr + 1))
        _noOfEventsSinceLastSnapshot = _noOfEventsSinceLastSnapshot + 1
        super.aroundReceive(behaviour, event)

      case EventEnvelope(offset, persistenceId, sequenceNr, event) ⇒
        _lastOffset = Sequence(offset)
        sequenceNrByPersistenceId = sequenceNrByPersistenceId + (persistenceId -> (sequenceNr + 1))
        _noOfEventsSinceLastSnapshot = _noOfEventsSinceLastSnapshot + 1
        super.aroundReceive(behaviour, event)

      case LiveStreamFailed(ex) ⇒
        log.error(ex, s"event=live_stream_failed $logContext")
        // We have to crash the actor
        throw ex

      case SaveSnapshotSuccess(metadata) ⇒
        snapshotSaved(metadata)

      case SaveSnapshotFailure(metadata, error) ⇒
        snapshotFailed(metadata, error)

      case _ ⇒
        _noOfEventsSinceLastSnapshot = _noOfEventsSinceLastSnapshot + 1
        super.aroundReceive(behaviour, msg)
    }
  }

  private def recovering(behaviour: Receive, msg: Any) = {
    msg match {
      case EventEnvelope(offset, persistenceId, sequenceNr, event) ⇒
        _lastOffset = Sequence(offset)
        sequenceNrByPersistenceId = sequenceNrByPersistenceId + (persistenceId -> (sequenceNr + 1))
        _noOfEventsSinceLastSnapshot = _noOfEventsSinceLastSnapshot + 1
        super.aroundReceive(behaviour, event)

      case EventEnvelope2(offset, persistenceId, sequenceNr, event) ⇒
        _lastOffset = offset
        sequenceNrByPersistenceId = sequenceNrByPersistenceId + (persistenceId -> (sequenceNr + 1))
        _noOfEventsSinceLastSnapshot = _noOfEventsSinceLastSnapshot + 1
        super.aroundReceive(behaviour, event)

      case QueryView.RecoveryCompleted ⇒
        log.debug(s"event=recovery_completed recovery_duration_ms=${System.currentTimeMillis() - recoveryStartedAt} $logContext")
        startLive()

      case RecoveryFailed(ex) ⇒
        log.error(ex, s"event=recovery_failed recovery_duration_ms=${System.currentTimeMillis() - recoveryStartedAt} $logContext")
        // We have to crash the actor
        throw ex

      case SaveSnapshotSuccess(metadata) ⇒
        snapshotSaved(metadata)

      case SaveSnapshotFailure(metadata, error) ⇒
        snapshotFailed(metadata, error)

      case _: Any ⇒
        recoveringStash.stash()
    }
  }

  private def waitingForSnapshot(behaviour: Receive, msg: Any) = {
    msg match {
      case LoadSnapshotResult(Some(SelectedSnapshot(metadata, status: RecoveringStatus[_])), _) ⇒
        val offer = SnapshotOffer(metadata, status.data)
        if (behaviour.isDefinedAt(offer)) {
          super.aroundReceive(behaviour, offer)
          _lastOffset = status.maxOffset
          sequenceNrByPersistenceId = status.sequenceNrs
          lastSnapshotSequenceNr = metadata.sequenceNr
          log.debug(s"event=snapshot_loaded $logContext")
        }
        startRecovery()

      case LoadSnapshotResult(None, _) ⇒
        log.debug(s"event=snapshot_not_loaded $logContext")
        startRecovery()

      case LoadSnapshotFailed(ex) ⇒
        // It is recoverable so we don't need to crash the actor
        log.error(ex, s"event=load_snapshot_failed $logContext")
        startRecovery()

      case message: Any ⇒
        recoveringStash.stash()
    }
  }

  private def startRecovery(): Unit = {

    loadSnapshotTimer.foreach(_.cancel())
    currentState = State.Recovering

    val stream = recoveryTimeout match {
      case t: FiniteDuration ⇒ recoveringStream().completionTimeout(t)
      case _ ⇒ recoveringStream()
    }

    // TODO runForeach is not great because it does not apply backpressure. ask is not great as well because it will
    // instantiate an actor for each message. Good solution will be to have an ActorSubscriber that will forward message
    // to this actor that will reply for each message.
    stream
      .concat(Source.single(QueryView.RecoveryCompleted))
      .runForeach(self ! _)
      .recover {
        case NonFatal(ex) ⇒ RecoveryFailed(ex)
      }
      .pipeTo(self)
  }

  private def startLive(): Unit = {
    log.info(s"event=start_live $logContext")

    preLive()

    currentState = State.Live
    recoveringStash.unstashAll()

    // TODO runForeach is not great because it does not apply backpressure. ask is not great as well because it will
    // instantiate an actor for each message. Good solution will be to have an ActorSubscriber that will forward message
    // to this actor that will reply for each message.
    val liveResult = liveStream()
      // The liveStream should never complete, it is a guard here.
      .concat(Source.single(LiveStreamFailed(LiveStreamCompleted)))
      .runForeach(self ! _)

    liveResult
      .recover {
        // Hopefully the JVM crash on fatal exception :)
        case NonFatal(ex) ⇒
          LiveStreamFailed(ex)
      }
      .pipeTo(self)
  }

  final def snapshotStatus(status: Any): Unit = if (!isSavingSnapshot()) {
    saveSnapshot(RecoveringStatus(status, _lastOffset, Map.empty))
    log.debug(s"event=save_snapshot_requested $logContext")
    _isSavingSnapshot = true
  } else {
    log.debug(s"event=save_snapshot_skipped $logContext")
  }

  private def snapshotSaved(metadata: SnapshotMetadata): Unit = {
    lastSnapshotSequenceNr = metadata.sequenceNr
    _noOfEventsSinceLastSnapshot = 0L
    _isSavingSnapshot = false
    log.debug(s"event=save_snapshot_succeeded $logContext")
  }

  private def snapshotFailed(metadata: SnapshotMetadata, error: Throwable): Unit = {
    _isSavingSnapshot = false
    log.error(error, s"event=save_snapshot_failed $logContext")
  }

  private def logContext: String = {
    s"""state=$currentState last_offset=$lastOffset is_saving_snapshot=${isSavingSnapshot} last_snapshot_sequence_nr=$lastSnapshotSequenceNr no_of_event_since_last_snapshot=${noOfEventSinceLastSnapshot()}"""
  }

}
