package com.github.bmorris458.market.processors

import akka.actor.{Actor, ActorRef}

/*
EchoActor serves queries.
  Any query that is a simple string gets printed to the terminal for debug purposes.
  Event objects are handled by `updateWith`
  Query objects are handled by `lookup`
  Shutdown object triggers actor stop
  Anything else replies with a None (which should trigger a 404)
*/
class EchoActor extends Actor {
  var users = Map[String, User]()
  var items = Map[String, Item]()

  def receive = {
    case message: String => println(s"MrEko: $message")
    case event: Event => updateWith(event)
    case GetUser(id) => sender ! lookupUser(id)
    case GetItem(id) => {
      var returnAddress = sender
      lookupItem(id) match {
        case Some(item) => {
          println(s"Found item $item")
          //This is working on first request, then failing on subsequent. Is the returnAddress getting stashed and stale?
          returnAddress ! item
        }
        case None => returnAddress ! s"No item found for ${id}"
      }
    }
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
      case e: UserAdded => { users = users + (e.id -> User(e.id, e.version, e.name)); println(s"Adding user with id: ${e.id}") }
      case e: UserRemoved => { users = users - e.id; println(s"Removing user with id: ${e.id}") }
      case e: ItemAdded => { items = items + (e.id -> Item(e.id, e.version, e.title)); println(s"Adding item with id: ${e.id}") }
      case e: ItemRemoved => { items = items - e.id; println(s"Removing item with id: ${e.id}") }
    }
  }

  def lookupUser(id: String): Option[User] = users get id
  def lookupItem(id: String): Option[Item] = items get id
}
