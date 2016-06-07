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
import processors.common._

/* * * * * * * * * * * * * * * * * * * * * * * * *
Reaper to ensure clean shutdown of system
if  the CommandProcessor is terminated.
 * * * * * * * * * * * * * * * * * * * * * * * * */
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
    context.system.terminate()
  }
}

/* * * * * * * * * * * * * * * * * * * * * * * * *
Tester to populate the database with some objects
to query.
 * * * * * * * * * * * * * * * * * * * * * * * * */
/*
case object Populate

class Tester(target: ActorRef) extends Actor {
  def receive = {
    case Populate => {
      HttpRequest(PUT, "/users/add?id=123&name=Ben")
      HttpRequest(PUT, "/users/add?id=123&name=Ben")
      HttpRequest(PUT, "/users/add?id=123&name=Ben")
      HttpRequest(PUT, "/users/add?id=123&name=Ben")
    }
    case _ =>
  }
}
*/

/* * * * * * * * * * * * * * * * * * * * * * * * *
ActorSystem spins up Sarge, Otto, and MrEko.
 * * * * * * * * * * * * * * * * * * * * * * * * */
class MyServiceActor extends Actor with HttpService {
  import Reaper._
  implicit val timeout: Timeout = 5.seconds

  def actorRefFactory = context
  val grimReaper = actorRefFactory.system.actorOf(Props[GrimReaper], "Otto")

  val echoActor = actorRefFactory.system.actorOf(Props[EchoActor], "MrEko")
  //grimReaper ! WatchMe(echoActor)

  val cmdProcessor = actorRefFactory.system.actorOf(Props[CommandProcessor], "Sarge")
  grimReaper ! WatchMe(cmdProcessor)

  def receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        val indexResponseFuture = echoActor ? GetIndex
        //There's probably a more idiomatic way to do this, but this works
        var respString = ""
        indexResponseFuture.onSuccess {
          case reply: String => respString = reply
        }
        complete { respString }
      }
    } ~
    path("users") {
      get {
        complete { s"Command issued: Get all users." }
      }
    } ~
    path("users" / "add") {
      //example: localhost:8080/users/add?id=123&name=Ben
      parameters('id, 'name ? "JoeSmith") { (id, name) =>
        complete { s"Sending command: Add user $id: $name" }
      }
    } ~
    path("users" / Segment) { userId =>
      get {
        complete { s"Get user entry corresponding to ID: ${userId}" }
      }
    } ~
    path("items") {
      get {
        complete { s"Get all items." }
      }
    } ~
    path("items" / "add") {
      //example: localhost:8080/items/add?id=123&title=Testing
      parameters('id, 'title ? "testy") { (id, title) =>
        complete { s"Sending command: Add user $id: $title" }
      }
    } ~
    path("items" / Segment) { itemId =>
      get {
        complete { s"Get item entry corresponding to ID: ${itemId}" }
      }
    } ~
    path("stop") {
      get {
        complete {
          //echoActor ! Shutdown
          cmdProcessor ! Shutdown
          "Stop message sent"
        }
      }
    }
}
