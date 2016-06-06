package com.github.bmorris458.market

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import akka.actor.{Actor, ActorRef, Terminated, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.routing._
import spray.http._
import MediaTypes._

import processors._

case object GetIndex

class EchoActor extends Actor {
  def receive = {
    case msg: String => println(s"MrEcho: $msg")
    case Shutdown => {
      println("MrEcho: Got the Shutdown message")
      context.stop(self)
    }
    case GetIndex => sender ! "Welcome to the Market"
    case _ => {
      println("MrEcho: Received an unexpedted object")
      sender ! "Error: Unknown command on MrEcho"
    }
  }
}

object Reaper {
  case class WatchMe(ref: ActorRef)
}
abstract class Reaper extends Actor {
  import Reaper._

  val watched = ArrayBuffer.empty[ActorRef]

  def allSoulsReaped(): Unit

  final def receive = {
    case WatchMe(ref) =>
      context.watch(ref)
      watched += ref
    case Terminated(ref) =>
      watched -= ref
      if(watched.isEmpty) allSoulsReaped()
  }
}
class GrimReaper extends Reaper {
  def allSoulsReaped(): Unit = {
    println("Reaper: All watched actors dead. Shutting down actor system.")
    context.system.shutdown()
  }
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with HttpService {
  import Reaper._
  implicit val timeout: Timeout = 5.seconds
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context
  val grimReaper = actorRefFactory.system.actorOf(Props[GrimReaper], "TheReapersGrim")

  val echoActor = actorRefFactory.system.actorOf(Props[EchoActor], "MrEcho")
  grimReaper ! WatchMe(echoActor)

  //val userProcessor = actorRefFactory.system.actorOf(Props[UserProcessor], "user-processor")
  //grimReaper ! WatchMe(userProcessor)

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          val indexResponseFuture = echoActor ? GetIndex
          //There's probably a more idiomatic way to do this, but this works
          var respString = ""
          indexResponseFuture.onSuccess {
            case reply: String => respString = reply
          }
          complete { respString }
        }
      }
    } ~
    path("users") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            s"Command issued: Get all users."
          }
        }
      }
    } ~
    path("users" / "add") {
      //example: localhost:8080/users/add?id=123&name=Ben
      parameters('id, 'name ? "JoeSmith") { (id, name) =>
        complete {
          s"Sending command: Add user $id: $name"
        }
      }
    } ~
    path("users" / Segment) { userId =>
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            s"Get user entry corresponding to ID: ${userId}"
          }
        }
      }
    } ~
    path("items") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete { s"Get all items." }
        }
      }
    } ~
    path("items" / Segment) { itemId =>
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete { s"Get item entry corresponding to ID: ${itemId}" }
        }
      }
    } ~
    path("stop") {
      get {
        complete {
          echoActor ! Shutdown
          //userProcessor ! Shutdown
          "Stop message sent"
        }
      }
    }
}
