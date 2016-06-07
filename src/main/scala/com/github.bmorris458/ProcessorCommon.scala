package com.github.bmorris458.market.processors

import akka.actor.ActorRef

/**** Actor Control Objects ****/
case object Shutdown
case class Hello(ref: ActorRef)
case object SayHello

/**** Query System Objects ****/
trait UserQuery
case class GetUser(id: String) extends UserQuery

trait ItemQuery
case class GetItem(id: String) extends ItemQuery

/**** Event System Objects ****/

sealed trait Event {
  def id: String
  def version: Long
}

trait Record
case class User(id: String, version: Long, name: String) extends Record
case class Item(id: String, version: Long, title: String) extends Record

/* * * * * * * * * * * * * * * * * * * * * * * * *
User-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class UserAdded(id: String, version: Long, name: String) extends Event
case class UserRemoved(id: String, version: Long) extends Event

/* * * * * * * * * * * * * * * * * * * * * * * * *
Item-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class ItemAdded(id: String, version: Long, title: String) extends Event
case class ItemRemoved(id: String, version: Long) extends Event
