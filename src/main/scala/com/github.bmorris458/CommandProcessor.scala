package com.github.bmorris458.market.processors

//Base applicative validation portion on CQRS architecture as demonstrated in https://github.com/ironfish/akka-persistence-mongo-samples/tree/master/mongo-cqrs-es-app
//Actors and messaging system based on design patterns in "Reactive Messaging Patterns with the Actor Model", Vaugh Vernon, 2016

import scala.concurrent.ExecutionContext.Implicits.global
import akka.persistence.PersistentActor
import akka.actor.{ActorRef, Props}

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
  val queryProcessor = context.actorOf(Props[EchoActor], "MrEko")

  override def receiveCommand: Receive = {
    case SayHello => sender ! Hello(queryProcessor) //Introduce Guardian to MrEko, so Guardian knows where to send queries
    case "Please introduce yourself" => sender ! Hello(queryProcessor) //Guardian uninitialized, reintroduce MrEko
    case cmd: AddUser =>
      persist(UserAdded(cmd.id, cmd.expectedVersion, cmd.name)) { event =>
        sendEvent(event)
      }
    case cmd: RemoveUser =>
      persist(UserRemoved(cmd.id, cmd.expectedVersion)) { event =>
        sendEvent(event)
      }
      case cmd: AddItem =>
        persist(ItemAdded(cmd.id, cmd.expectedVersion, cmd.title)) { event =>
          sendEvent(event)
        }
      case cmd: RemoveItem =>
        persist(ItemRemoved(cmd.id, cmd.expectedVersion)) { event =>
          sendEvent(event)
        }    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  override def receiveRecover: Receive = {
    case evt: Event => sendEvent(evt)
    case Shutdown => {
      println("ES Actor: Got the Shutdown message")
      context.stop(self)
    }
  }

  def sendEvent[E <: Event](event: E) = {
    queryProcessor ! event
  }
}
