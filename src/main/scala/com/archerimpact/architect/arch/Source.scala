package com.archerimpact.architect.arch

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.arch.Source.StartSending

object Source {
  final case object StartSending
}

class Source[OutType](
                       next: Seq[ActorRef],
                       run: (OutType => Unit, ActorContext) => Unit
                     ) extends Actor with ActorLogging {

  def send(output: OutType): Unit =
    for (n <- next)
      n ! output

  override def receive: Receive = {
    case StartSending => run(send, context)
  }
}

abstract class SourceSpec {
  type OutType
  def run(send: OutType => Unit, context: ActorContext): Unit
  def props(next: Seq[ActorRef]): Props = Props(new Source[OutType](next, run))
  def instantiate(context: ActorContext, next: Seq[ActorRef]): ActorRef = context.actorOf(props(next))
}

