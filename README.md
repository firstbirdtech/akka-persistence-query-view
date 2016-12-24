Persistence query view
======================
The `QueryView` is a replacement of the deprecated `PersistentView` in Akka Persistence module.

## Anatomy of a Persistence QueryView
The Persistence query view has three possible state: `WaitingForSnapshot`, `Recovering` and `Live`. 

It always start in `WaitingForSnapshot` state where it is waiting to receive a previously saved snapshot. When the snapshot has been loaded or failed to load, the view switch to the `Recovering` state. 
During the `Recovering` state it will receive all the past events from the journal. When all the existing event from the journal have been consumed, the view will switch to the `Live` state which will keep until the actor stop. 
During the `Live` events the view will consume live events from the journal and external messages.

When the view is in `WaitingForSnapshot` or `Recovering` it will not reply to any messages, but will stash them waiting to switch to the `Live` state where these message will be processed.

## How to implement
It is up to the implementor defining the queries used during the `Recovering` and `Live` states. Generally they will be the same query, with the difference that the recovery one is a finite stream while the live one is infinite. 

The `WaitingForSnapshot` and `Recovering` states are protected by a timeout, if the view will not be able to recuild its status within this timeout, it will switch to the `Live` state or crash. This behavior is controlled by the `recovery-timeout-strategy` (TODO) option.


