package com.github.bmorris458.market

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import akka.actor.{Actor, ActorRef, Terminated, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.util.{Success, Failure}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import spray.routing._
import spray.http._
import MediaTypes._

import processors._

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
ActorSystem spins up Sarge, Otto, and MrEko.
 * * * * * * * * * * * * * * * * * * * * * * * * */
class MyServiceActor extends Actor with HttpService {
  import Reaper._
  implicit val timeout: Timeout = 5.seconds

  def actorRefFactory = context
  val grimReaper = actorRefFactory.system.actorOf(Props[GrimReaper], "Otto")

  //val echoActor = actorRefFactory.system.actorOf(Props[EchoActor], "MrEko")
  val cmdProcessor = actorRefFactory.system.actorOf(Props[CommandProcessor], "Sarge")
  grimReaper ! WatchMe(cmdProcessor) //Set Otto's shutdown hook on Sarge

  //Send command to get MrEko's ActorRef
  var echoActor: ActorRef = _
  //@todo: Not sure if this is necessary. Test commenting it out once other functionality is stable.
  override def preStart(): Unit = {
     context.become(receiveEchoActor)
   }
  cmdProcessor ! SayHello

  def receive = receiveEchoActor

  def receiveEchoActor: Receive = {
    case Hello(ref) => {
      echoActor = ref
      println("Guardian: Ready to receive routes")
      context.become(receiveRoutes)
    }
    case _ => sender ! "Please introduce yourself"
  }

  def receiveRoutes: Receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete(index)
        }
      }
    } ~
    path("users") {
      get {
        complete {

          s"Command issued: Get all users."
        }
      }
    } ~
    path("users" / "add") {
      //example: localhost:8080/users/add?id=123&name=Ben
      parameters('id, 'name ? "JoeSmith") { (id, name) =>
        complete {
          cmdProcessor ! AddUser(id, 0L, name)
          s"Sending command: Add user $id: $name"
        }
      }
    } ~
    path("users" / Segment) { userId =>
      get {
        complete {
          var userResponseFuture = echoActor ? GetUser(userId)
          Await.result(userResponseFuture, timeout.duration).toString
        }
      }
    } ~
    path("items") {
      get {
        complete {

          s"Get all items."
        }
      }
    } ~
    path("items" / "add") {
      //example: localhost:8080/items/add?id=123&title=Testing
      parameters('id, 'title ? "testy") { (id, title) =>
        complete {
          cmdProcessor ! AddItem(id, 0L, title)
          s"Sending command: Add user $id: $title"
        }
      }
    } ~
    path("items" / Segment) { itemId =>
      get {
        complete {
          var itemResponseFuture = echoActor ? GetItem(itemId)
          Await.result(itemResponseFuture, timeout.duration).toString
        }
      }
    } ~
    path("stop") {
      get {
        complete {
          cmdProcessor ! Shutdown
          "Stop message sent"
        }
      }
    }

  lazy val index =
    """<html>
      <body>
        <h1>Welcome to the Market!</h1>
        <p>Test Links:</p>
        <ul>
          <li><a href="/users">Query all users</a></li>
          <li><a href="/users/add?id=a101&name=Ben">Add user Ben with ID a101</a></li>
          <li><a href="/users/add?id=a102&name=Sally">Add user Sally with ID a102</a></li>
          <li><a href="/users/a101">Query user a101</a></li>
          <li><a href="/users/a102">Query user a102</a></li>
          <li><a href="/users/remove?id=a101">Remove user Ben</a></li>
          <li><a href="/users/remove?id=a102">Remove user Sally</a></li>
          <li><a href="/items">Query all items</a></li>
          <li><a href="/items/add?id=q2101&title=Black Widow">Add item Black Widow with ID q2101</a></li>
          <li><a href="/items/add?id=q2102&title=Red Sonja">Add item Red Sonja with ID q2102</a></li>
          <li><a href="/items/q2101">Query item q2101</a></li>
          <li><a href="/items/q2102">Query item q2102</a></li>
          <li><a href="/items/remove?id=q2101">Remove item Black Widow</a></li>
          <li><a href="/items/remove?id=q2102">Remove item Red Sonja</a></li>
          <li><a href="/stop?method=post">Stop server</a></li>
        </ul>
      </body>
    </html>"""
}
