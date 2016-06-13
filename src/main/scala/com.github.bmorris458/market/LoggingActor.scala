package com.github.bmorris458.market.processors

import akka.actor.{Actor, ActorRef}

trait SuperImportant
trait KindaImportant
trait NotImportant

class LoggingActor extends Actor {
  def receive = {
    case msg: String => categorize(msg, sender)
    case _ =>
  }

  def categorize(msg: String, source: ActorRef) = {
    getPropOfSource(source) match {
      case SuperImportant => logging.critical(msg)
      case KindaImportant => logging.info(msg)
      case NotImportant => logging.debug(msg)
    }
  }
}
