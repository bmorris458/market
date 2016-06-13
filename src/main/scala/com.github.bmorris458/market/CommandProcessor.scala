package com.github.bmorris458.market.processors

//Base applicative validation portion on CQRS architecture as demonstrated in https://github.com/ironfish/akka-persistence-mongo-samples/tree/master/mongo-cqrs-es-app
//Actors and messaging system based on design patterns in "Reactive Messaging Patterns with the Actor Model", Vaugh Vernon, 2016

import scala.concurrent.ExecutionContext.Implicits.global
import akka.persistence.PersistentActor
import akka.actor.{ActorRef, Props}

sealed trait Command {
  def id: String
  def expectedVersion: Long
}
case class AddUser(id: String, expectedVersion: Long, name: String) extends Command
case class RemoveUser(id: String, expectedVersion: Long) extends Command
case class AddUserTag(id: String, expectedVersion: Long, tag: String) extends Command
case class RemoveUserTag(id: String, expectedVersion: Long, tag: String) extends Command

case class AddItem(id: String, expectedVersion: Long, title: String) extends Command
case class RemoveItem(id: String, expectedVersion: Long) extends Command
case class AddItemTag(id: String, expectedVersion: Long, tag: String) extends Command
case class RemoveItemTag(id: String, expectedVersion: Long, tag: String) extends Command

// Starting from the persistent actor template in "Reactive Messaging Patterns" p. 355
class CommandProcessor extends PersistentActor {
  override def persistenceId = "Sarge"
  val queryProcessor = context.actorOf(Props[EchoActor], "MrEko")
  val publisher = context.actorOf(Props[NotificationPublisher], "Gutenburg")

  val cToEMap: Map[Command,Event] = ...
  def validate(c: Command): Event = {
    cToEMap(c)
  }
  def validateAndForward(cmd: Command) = {
    persist(validate(cmd))(sendEvent)
  }
  override def receiveCommand: Receive = {
    case SayHello => sender ! Hello(queryProcessor) //Introduce Guardian to MrEko, so Guardian knows where to send queries
    case "Please introduce yourself" => sender ! Hello(queryProcessor) //Guardian uninitialized, reintroduce MrEko
    case AddUser(id, ev, name) =>
      persist(UserAdded(id, ev, name)) { event =>
        sendEvent(event)
      }
    //Tag commands should really include a query and validation to ensure that the target exists.
    case cmd: AddUserTag =>
      persist(UserTagAdded(cmd.id, cmd.expectedVersion, cmd.tag)) { event =>
        sendEvent(event)
      }
    case cmd: RemoveUserTag =>
      persist(UserTagRemoved(cmd.id, cmd.expectedVersion, cmd.tag)) { event =>
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
      //Tag commands should really include a query and validation to ensure that the target exists.
      case cmd: AddItemTag => {
        publisher ! NewItemTagAlert(cmd.id, cmd.tag)
        persist(ItemTagAdded(cmd.id, cmd.expectedVersion, cmd.tag)) { event =>
          sendEvent(event)
        }
      }
      case cmd: RemoveItemTag =>
        persist(ItemTagRemoved(cmd.id, cmd.expectedVersion, cmd.tag)) { event =>
          sendEvent(event)
        }
      case cmd: RemoveItem => {
        publisher ! ItemSoldAlert(cmd.id)
        persist(ItemRemoved(cmd.id, cmd.expectedVersion)) { event =>
          sendEvent(event)
        }
      }
      case Shutdown => {
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
