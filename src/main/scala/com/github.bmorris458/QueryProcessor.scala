package com.github.bmorris458.market.processors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Map
import scala.concurrent.Await
import akka.pattern.ask
import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import scala.concurrent.duration._

/*
EchoActor serves queries.
  Any query that is a simple string gets printed to the terminal for debug purposes.
  Event objects are handled by `updateWith`
  Query objects are handled by `lookup`
  Shutdown object triggers actor stop
  Anything else replies with a None (which should trigger a 404)
*/
class EchoActor extends Actor {
  implicit val timeout: Timeout = 5.seconds

  var users = Map[String, User]()
  var items = Map[String, Item]()

  def receive = {
    case message: String => println(s"MrEko: $message")
    case event: Event => updateWith(event)
    //The first incoming query is responding correctly, but all subsequent futures
    //fail to complete.
    case GetUser(id) => sender() ! lookupUser(id)
    case GetAllUsers => sender() ! allUsers()
    case GetItem(id) => sender() ! lookupItem(id)
    case GetAllItems => sender() ! allItems()
    case GetNotifications(id) => {
      //Save sender in case multiple request come it (using sender directly could cause mis-routing)
      val requestor = sender
      requestor ! getNotes(id)
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
      case e: UserAdded => {
        users = users + (e.id -> User(e.id, e.version, e.name, Set[String](), context.actorOf(Props[Subscriber])))
        println(s"MrEko: Adding user with id: ${e.id}")
      }
      case e: UserRemoved => {
        users = users - e.id
        println(s"MrEko: Removing user with id: ${e.id}")
      }
      case e: UserTagAdded => (users get e.id) match {
        case Some(u) => {
          users = users + (e.id -> User(u.id, e.version, u.name, u.tags + e.tag, u.watcher))
          u.watcher ! WatchTag(e.tag)
          println(s"MrEko: Adding tag ${e.tag} to user with id: ${e.id}") }
        case None =>
      }
      case e: UserTagRemoved => (users get e.id) match {
        case Some(u) => {
          users = users + (e.id -> User(u.id, e.version, u.name, u.tags - e.tag, u.watcher))
          u.watcher ! UnwatchTag(e.tag)
          println(s"MrEko: Removing tag ${e.tag} from user with id: ${e.id}") }
        case None =>
      }
      case e: ItemAdded => {
        items = items + (e.id -> Item(e.id, e.version, e.title, Set[String]()))
        println(s"MrEko: Adding item with id: ${e.id}")
      }
      case e: ItemRemoved => {
        items = items - e.id
        println(s"MrEko: Removing item with id: ${e.id}")
      }
      case e: ItemTagAdded => {
        val item = items get e.id
        item match {
          case Some(i) => { items = items + (e.id -> Item(i.id, e.version, i.title, i.tags + e.tag)); println(s"MrEko: Adding tag ${e.tag} to item with id: ${e.id}") }
          case None =>
        }
      }
      case e: ItemTagRemoved => (items get e.id) match {
        case Some(i) => { items = items + (e.id -> Item(i.id, e.version, i.title, i.tags - e.tag)); println(s"MrEko: Removing tag ${e.tag} from item with id: ${e.id}") }
        case None =>
      }
    }
  }

  def lookupUser(id: String): String = (users get id) match {
    case Some(u) => u.toString
    case None => "No user found for that ID"
  }
  def allUsers(): List[String] = users.values.toList.map( x => x.toString).sorted
  def lookupItem(id: String): String = (items get id) match {
    case Some(i) => i.toString
    case None => "No item found for that ID"
  }
  def allItems(): List[String] = items.values.toList.map(x => x.toString).sorted

  def getNotes(id: String): String = (users get id) match {
    case Some(u) => {
      var notesF = u.watcher ? PullNotes
      Await.result(notesF, timeout.duration) match {
        case ls: List[Notification] @unchecked => ls.mkString("\n")
        case _ => "Unexpected response type"
      }
    }
    case None => "User not found"
  }
}
