package com.github.bmorris458.market.processors

//A market is a map of keys to objects that inherit from Marketable.
//Each market can only have 1 type of marketable in it.
//Market supports getting, setting, and removing

//Basing on CQRS architecture as demonstrated in https://github.com/ironfish/akka-persistence-mongo-samples/tree/master/mongo-cqrs-es-app

import akka.persistence.PersistentActor
import common._
import scalaz._
import Scalaz._

/* * * * * * * * * * * * * * * * * * * * * * * * *
User-related commands, events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class User(id: String, version: Long, name: String)

sealed trait UserCommand {
  def id: String
  def expectedVersion: Long
}
case class AddUser(id: String, expectedVersion: Long, name: String) extends UserCommand
case class RemoveUser(id: String, expectedVersion: Long) extends UserCommand

sealed trait UserEvent {

  def id: String
  def version: Long
}
case class UserAdded(id: String, version: Long, name: String) extends UserEvent
case class UserRemoved(id: String, version: Long) extends UserEvent

/* * * * * * * * * * * * * * * * * * * * * * * * *
Item-related commands, events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class Item(id: String, version: Long, title: String)

sealed trait ItemCommand {
 def id: String
 def expectedVersion: Long
}
case class AddItem(id: String, expectedVersion: Long, title: String) extends ItemCommand
case class RemoveItem(id: String, expectedVersion: Long) extends ItemCommand

sealed trait ItemEvent {
 def id: String
 def version: Long
}
case class ItemAdded(id: String, version: Long, title: String) extends ItemEvent
case class ItemRemoved(id: String, version: Long) extends ItemEvent


// Starting from the persistent actor template in "Reactive Messaging Patterns" p. 355
class CommandProcessor extends PersistentActor {
  override def persistenceId = "processor"
  var users = Map[String, User]()
  var items = Map[String, Item]()

  override def receiveCommand: Receive = {
    case cmd: AddUser => persist(UserAdded(cmd.id, cmd.expectedVersion, cmd.name)) { event => updateWith(event) }
    case cmd: RemoveUser => persist(UserRemoved(cmd.id, cmd.expectedVersion)) { event => updateWith(event) }
    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  // Not very dry. Fix once everything is running as expected.
  override def receiveRecover: Receive = {
    case evt: UserAdded => updateWith(evt)
    case evt: UserRemoved => updateWith(evt)
    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  def updateWith[E <: UserEvent](event: E) = {
    event match {
      case e: UserAdded => users = users + (e.id -> User(e.id, e.version, e.name))
      case e: UserRemoved => users = users - e.id
    }
  }
}
