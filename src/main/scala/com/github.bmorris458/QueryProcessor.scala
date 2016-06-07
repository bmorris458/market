package com.github.bmorris458.market.processors

import akka.actor.{Actor, ActorRef}
import common._

/*
EchoActor serves queries.
  Any query that is a simple string gets printed to the terminal for debug purposes.
  Event objects are handled by `updateWith`
  Query objects are handled by `lookup`
  Shutdown object triggers actor stop
  GetIndex returns packet needed to render landing page
  Anything else replies with a None (which spray interprets as a 404)
*/
class EchoActor extends Actor {
  var users = Map[String, User]()
  var items = Map[String, Item]()

  def receive = {
    case message: String => println(s"MrEko: $message")
    case GetIndex => sender ! "Welcome to the Market"
    case event: Event => updateWith(event)
    case query: Query => lookup(sender, query)
    case Shutdown => {
      println("MrEko: Got the Shutdown message")
      context.stop(self)
    }
    case _ => {
      println("MrEko: Received an unexpedted object")
      sender ! None
    }
  }

  def updateWith[E <: Event](event: E) = {
    event match {
      case e: UserAdded => users = users + (e.id -> User(e.id, e.version, e.name))
      case e: UserRemoved => users = users - e.id
      case e: ItemAdded => items = items + (e.id -> Item(e.id, e.version, e.title))
      case e: ItemRemoved => items = items - e.id
    }
  }

  def lookup[Q <: Query](sender: ActorRef, query: Q) = {
    //Map get returns an option
    val response = query match {
      case q: GetUser => users get q.id
      case q: GetItem => items get q.id
    }
    response match {
      case Some(tgt) => sender ! tgt
      case None => sender ! None
    }
  }
}
