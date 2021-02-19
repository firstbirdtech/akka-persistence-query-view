This is a fork of [danischroeter/akka-persistence-query-view](https://github.com/danischroeter/akka-persistence-query-view) which uses akka version 2.5.x instead of 2.4.x.

# Persistence query view

[![Maven Central](https://img.shields.io/maven-central/v/com.firstbird/akka-persistence-query-view_2.13.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.firstbird%22%20AND%20a:%22akka-persistence-query-view_2.13%22)
[![Github Actions CI Workflow](https://github.com/firstbirdtech/akka-persistence-query-view/workflows/CI/badge.svg)](https://github.com/firstbirdtech/akka-persistence-query-view/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/firstbirdtech/akka-persistence-query-view/branch/master/graph/badge.svg?token=3WIUBL914B)](https://codecov.io/gh/firstbirdtech/akka-persistence-query-view)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

The `QueryView` is a replacement of the deprecated `PersistentView` in Akka Persistence module.

## Anatomy of a Persistence QueryView

The Persistence query view has three possible state: `WaitingForSnapshot`, `Recovering` and `Live`.

It always start in `WaitingForSnapshot` state where it is waiting to receive a previously saved snapshot. When the snapshot has been loaded or failed to load, the view switch to the `Recovering` state.
During the `Recovering` state it will receive all the past events from the journal. When all the existing event from the journal have been consumed, the view will switch to the `Live` state which will keep until the actor stop.
During the `Live` events the view will consume live events from the journal and external messages.

When the view is in `WaitingForSnapshot` or `Recovering` it will not reply to any messages, but will stash them waiting to switch to the `Live` state where these message will be processed.

## Adding the dependency

Add a dependency to your `build.sbt`:

```
libraryDependencies += "com.firstbird" %% "akka-persistence-query-view" % "x.x.x"
```

## How to implement

The first step is to define a `Querysupport` trait for your `ReadJournal` plugin. The LevelDb one is included:

```scala
import akka.contrib.persistence.query.QuerySupport
import akka.persistence.QueryView
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

trait LevelDbQuerySupport extends QuerySupport { this: QueryView =>

  override type Queries = LeveldbReadJournal
  override def firstOffset: Offset = Offset.sequence(1L)
  override val queries: LeveldbReadJournal =
    PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
}
```

It is up to the implementor defining the queries used during the `Recovering` and `Live` states. Generally they will be the same query, with the difference that the recovery one is a finite stream while the live one is infinite.
Your `Queryview` implemention has to mix in one `QuerySupport` trait as well:

```scala
import akka.stream.scaladsl.Source

case class Person(name: String, age: Int)
case class PersonAdded(person: Person)
case class PersonRemoved(person: Person)

class PersonsQueryView extends QueryView with LevelDbQuerySupport {

  override val snapshotterId: String = "people"

  private var people: Set[Person] = Set.empty

  override def recoveringStream(): Source[AnyRef, _] =
    queries.currentEventsByTag("person", lastOffset)

  override def liveStream(): Source[AnyRef, _] =
    queries.eventsByTag("person", lastOffset)

  override def receive: Receive = {

    case PersonAdded(person) =>
      people = people + person

    case PersonRemoved(person) =>
      people = people - person

  }
}
```

The `WaitingForSnapshot` and `Recovering` states are protected by a timeout, if the view will not be able to rebuild its status within this timeout, it will switch to the `Live` state or crash. This behavior is controlled by the `recovery-timeout-strategy` (TODO) option.

The `QueryView` has an out-of-the-box support for snapshot. It is the same as the deprecated `PersistentView`, in the previous exaple to save a snapshot of the current people:

```scala
import akka.stream.scaladsl.Source

case class Person(name: String, age: Int)
case class PersonAdded(person: Person)
case class PersonRemoved(person: Person)

class PersonsQueryView extends QueryView with LevelDbQuerySupport {

  override val snapshotterId: String = "people"

  private var people: Set[Person] = Set.empty

  override def recoveringStream(): Source[AnyRef, _] =
    queries.currentEventsByTag("person", lastOffset)

  override def liveStream(): Source[AnyRef, _] =
    queries.eventsByTag("person", lastOffset)

  override def receive: Receive = {

    case PersonAdded(person) =>
      people = people + person
      if(noOfEventSinceLastSnapshot() > 100) {
        saveSnapshot(people)
      }

    case PersonRemoved(person) =>
      people = people - person
      if(noOfEventSinceLastSnapshot() > 100) {
        saveSnapshot(people)
      }

  }
}
```

Under the hood it will store also the last consumed offset and the last sequence number for each persistence id already consumed.

The QueryView checks that all received events follow a strict sequence per persistentId. Be aware that most journal plugins do not guarantee the correct order for `eventsByTag` (see journal documentation).
If that is ok one can overwrite `override def allowOutOfOrderEvents = true` to omit the checking. (In the future we might implement some deferred processing of out of order received events)

### Forced Update

Most journals use some sort of polling under the hood to support a live stream for `eventsByTag/eventsByPersistentId` PersistentQueries. (The default cassandra journal uses 3 seconds)
In scenarios when a more up to date state is needed one can issue a forced update which will immediately read from the recoveringStream. (Use `forceUpdate()` or send a ForceUpdate).
The QueryView ensures forcedUpdate is not performed concurrently so forceUpdate is ignored while it has not completed. After forceUpdate is completed `onForceUpdateCompleted()` is called.
For some scenarios it makes sense to retrigger `forceUpdate()` within `onForceUpdateCompleted()` until some condition is met.

## Future developments

-   Add the `recovery-timeout-strategy` option to control what to do when the view does ot recover within a certain amount of time.
