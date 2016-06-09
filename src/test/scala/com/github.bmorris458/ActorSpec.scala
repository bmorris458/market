package com.github.bmorris458.market

import org.scalatest.{BeforeAndAfterAll, WordSpecLike, Matchers}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActors, DefaultTimeout, ImplicitSender, TestKit}
import scala.concurrent.duration._
import scala.collection.immutable

import processors._

case object BogusObject

class ActorSpec
  extends TestKit(ActorSystem("ActorSpec",
    ConfigFactory.parseString(ActorSpec.config)))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
    import ActorSpec._

    //val reaper = system.actorOf(Props[Reaper], testActor) //Not exactly sure how best to test this. Would kill the system.
    val commander = system.actorOf(Props[CommandProcessor], "Sarge")
    val querrier = system.actorOf(Props[EchoActor], "MrEko")
    val publisher = system.actorOf(Props[NotificationPublisher], "Gutenburg")
    val subscriber = system.actorOf(Props[Subscriber], "DearReader")

    override def afterAll {
      shutdown()
    }

    "A CommandProcessor" should {

    }

    "An EchoActor" should {
      "Reply with a None when given an unexpected object" in {
        querrier ! BogusObject
        expectMsg(None)
      }
      "reply with a not found string if userId for query doesn't exist" in {
        querrier ! GetUser("123")
        expectMsg("No user found for that ID")
      }
      "reply with a not found string if itemId for query doesn't exist" in {
        querrier ! GetItem("123")
        expectMsg("No item found for that ID")
      }
      "stop in response to a Shutdown command" in {
        querrier ! Shutdown
        querrier ! BogusObject
        expectNoMsg
      }
    }

    "A Notify-Subscribe channel" should {
      "publish correctly formatted notifications to the event stream" in {

      }
      "read and store only notifications for subscribed tags" in {

      }
    }
  }

object ActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }
    """
}
