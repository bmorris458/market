package com.github.bmorris458.market.processors

import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.Await
import akka.actor.{Actor, ActorRef}
import scala.concurrent.duration._

/*
Alert->Notification system designed to work similarly to Command->Event
Alerts come in from the command processor (not the event processor, to keep
Notifications from being re-generaged on replay), and get checked for newness
and any other validation criteria. Valid Alerts are then converted into
Notifications and published. In this mockup, notifications are published to the
event stream, and each user has a Subscriber that keeps a copy of their sub tags,
and keeps notifications of new items with those tags, and all sold items.
*/
trait Alert {
  def itemId: String
}
case class NewItemTagAlert(itemId: String, tag: String) extends Alert
case class ItemSoldAlert(itemId: String) extends Alert

trait Notification
case class SubscriptionNotification(itemId: String, tag: String) extends Notification
case class ItemSoldNotification(itemId: String) extends Notification
case object PullNotes

class NotificationPublisher extends Actor {
  implicit val timeout: Timeout = 5.seconds

  def receive = {
    case alert: NewItemTagAlert => {
      println(s"Gutenburg: publishing $alert")
      context.system.eventStream.publish(SubscriptionNotification(alert.itemId, alert.tag))
    }
    case alert: ItemSoldAlert => {
      println(s"Gutenburg: publishing $alert")
      context.system.eventStream.publish(ItemSoldNotification(alert.itemId))
    }
    case _ => println("Gutenburg: Received something that isn't an alert...")
  }
}

class Subscriber extends Actor {
  implicit val timeout: Timeout = 5.seconds

  context.system.eventStream.subscribe(self, classOf[Notification])
  var watchedTags = Set[String]()
  var unreadNotes = List[Notification]()

  def receive = {
    case WatchTag(tag) => watchedTags = watchedTags + tag
    case UnwatchTag(tag) => watchedTags = watchedTags - tag
    case UnwatchAllTags => watchedTags = Set[String]()
    case n: SubscriptionNotification => if(watchedTags contains n.tag && !(unreadNotes contains n)) unreadNotes = n :: unreadNotes
    case n: ItemSoldNotification => unreadNotes = n :: unreadNotes //Right now all users get all ItemSoldNotifications. Could be improved, but works for now.
    case PullNotes => {
      println(s"Pulling notes: $unreadNotes")
      sender() ! unreadNotes
      unreadNotes = Nil
    }
  }
}
