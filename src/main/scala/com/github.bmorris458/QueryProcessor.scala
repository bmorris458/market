package com.github.bmorris458.market.processors

import scala.concurrent.ExecutionContext.Implicits.global
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
    //The first incoming query is responding correctly, but all subsequent futures
    //fail to complete.
    case GetUser(id) => sender() ! lookupUser(id)
    case GetItem(id) => sender() ! lookupItem(id)
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
      case e: UserAdded => { users = users + (e.id -> User(e.id, e.version, e.name)); println(s"MrEko: Adding user with id: ${e.id}") }
      case e: UserRemoved => { users = users - e.id; println(s"MrEko: Removing user with id: ${e.id}") }
      case e: ItemAdded => { items = items + (e.id -> Item(e.id, e.version, e.title)); println(s"MrEko: Adding item with id: ${e.id}") }
      case e: ItemRemoved => { items = items - e.id; println(s"MrEko: Removing item with id: ${e.id}") }
    }
  }

  def lookupUser(id: String): String = (users get id) match {
    case Some(u) => u.toString
    case None => "No user found for that ID"
  }
  def lookupItem(id: String): String = (items get id) match {
    case Some(i) => i.toString
    case None => "No item found for that ID"
  }
}
