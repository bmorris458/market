package com.github.bmorris458.market.processors

//Basing applicative validation portion on CQRS architecture as demonstrated in https://github.com/ironfish/akka-persistence-mongo-samples/tree/master/mongo-cqrs-es-app
//Actors and messaging system based on design patterns in "Reactive Messaging Patterns with the Actor Model", Vaugh Vernon, 2016

import akka.persistence.PersistentActor
import akka.actor.ActorRef
import common._
//import scalaz._
//import Scalaz._

sealed trait UserCommand {
  def id: String
  def expectedVersion: Long
}
case class AddUser(id: String, expectedVersion: Long, name: String) extends UserCommand
case class RemoveUser(id: String, expectedVersion: Long) extends UserCommand

sealed trait ItemCommand {
 def id: String
 def expectedVersion: Long
}
case class AddItem(id: String, expectedVersion: Long, title: String) extends ItemCommand
case class RemoveItem(id: String, expectedVersion: Long) extends ItemCommand

// Starting from the persistent actor template in "Reactive Messaging Patterns" p. 355
class CommandProcessor extends PersistentActor {
  override def persistenceId = "Sarge"
  var queryProcessor: ActorRef = _

  override def receiveCommand: Receive = {
    case Hello(ref) => queryProcessor = ref
    case cmd: AddUser =>
      persist(UserAdded(cmd.id, cmd.expectedVersion, cmd.name)) { event => sendEvent(event) }
    case cmd: RemoveUser =>
      persist(UserRemoved(cmd.id, cmd.expectedVersion)) { event => sendEvent(event) }
    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  // Not very dry. Fix once everything is running as expected.
  override def receiveRecover: Receive = {
    case evt: UserAdded => sendEvent(evt)
    case evt: UserRemoved => sendEvent(evt)
    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  def sendEvent[E <: Event](event: E) = {
    queryProcessor ! event
  }
}
