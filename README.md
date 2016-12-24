Persistence query view
======================
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
resolvers += Resolver.bintrayRepo("ovotech", "maven")
libraryDependencies += "com.ovoenergy" %% "akka-persistence-query-view" % "<version>"
```

## How to implement
The first step is to define a `Querysupport` trait for your `ReadJournal` plugin. The LevelDb one is included:
````scala
trait LevelDbQuerySupport extends QuerySupport { this: QueryView =>

  override type Queries = LeveldbReadJournal
  override def firstOffset: Offset = Offset.sequence(1L)
  override val queries: LeveldbReadJournal =
    PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
}
````

It is up to the implementor defining the queries used during the `Recovering` and `Live` states. Generally they will be the same query, with the difference that the recovery one is a finite stream while the live one is infinite. 
Your `Queryview` implemention has to mix in one `QuerySupport` trait as well:

```scala
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
saveSnapshot(people)
```

Under the hood it will store also the last consumed offset and the last sequence number for each persistence id already consumed.

## Future developments
 * Add the `recovery-timeout-strategy` option to control what to do when the view does ot recover within a certain amount of time.