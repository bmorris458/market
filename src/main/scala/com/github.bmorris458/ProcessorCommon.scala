package com.github.bmorris458.market.processors.common

import akka.actor.ActorRef

/**** Actor Control Objects ****/
case object Shutdown
case class Hello(ref: ActorRef)

/**** Query System Objects ****/
sealed trait Query
case object GetIndex extends Query
case class GetUser(id: String) extends Query
case class GetItem(id: String) extends Query

/**** Event System Objects ****/

sealed trait Event {
  def id: String
  def version: Long
}
/* * * * * * * * * * * * * * * * * * * * * * * * *
User-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class User(id: String, version: Long, name: String)

case class UserAdded(id: String, version: Long, name: String) extends Event
case class UserRemoved(id: String, version: Long) extends Event

/* * * * * * * * * * * * * * * * * * * * * * * * *
Item-related events, and other objects
 * * * * * * * * * * * * * * * * * * * * * * * * */
case class Item(id: String, version: Long, title: String)

case class ItemAdded(id: String, version: Long, title: String) extends Event
case class ItemRemoved(id: String, version: Long) extends Event
