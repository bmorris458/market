package com.github.bmorris458.market.processors

import akka.actor.ActorRef

/**** Actor Control Objects ****/
case object Shutdown
case object SayHello
case class Hello(ref: ActorRef)
case class SubscriptionNotification(userId: String, itemId: String)

/**** Query System Objects ****/
trait UserQuery
case object GetAllUsers extends UserQuery
case class GetUser(id: String) extends UserQuery
case class GetUsersWithTag(tag: String) extends UserQuery

trait ItemQuery
case object GetAllItems extends ItemQuery
case class GetItem(id: String) extends ItemQuery
case class GetItemsWithTag(tag: String) extends ItemQuery

/**** Event System Objects ****/
trait Record
case class User(id: String, version: Long, name: String, tags: Set[String]) extends Record
case class Item(id: String, version: Long, title: String, tags: Set[String]) extends Record

sealed trait Event {
  def id: String
  def version: Long
}

/* * * * * * * * * * * * * * * * * * * * * * * * *
User-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class UserAdded(id: String, version: Long, name: String) extends Event
case class UserRemoved(id: String, version: Long) extends Event
case class UserTagAdded(id: String, version: Long, tag: String) extends Event
case class UserTagRemoved(id: String, version: Long, tag: String) extends Event

/* * * * * * * * * * * * * * * * * * * * * * * * *
Item-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class ItemAdded(id: String, version: Long, title: String) extends Event
case class ItemRemoved(id: String, version: Long) extends Event
case class ItemTagAdded(id: String, version: Long, tag: String) extends Event
case class ItemTagRemoved(id: String, version: Long, tag: String) extends Event
