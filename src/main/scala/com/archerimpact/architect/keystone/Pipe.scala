package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}

class Pipe[InType, OutType](next: Seq[ActorRef], flow: InType => OutType) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Pipe Installed")
  override def postStop(): Unit = log.info("Pipe Removed")
  override def receive: Receive = {
    case input: InType @unchecked =>
      log.info("received message")
      val output: OutType = flow(input)
      for (n <- next)
        n ! output
  }
}

abstract class PipeSpec {
  type InType
  type OutType
  def flow(input: InType): OutType
  def props(next: Seq[ActorRef]): Props = Props(new Pipe[InType, OutType](next, flow))
  def instantiate(context: ActorContext, next: Seq[ActorRef]): ActorRef = context.actorOf(props(next), typeName(this))
}

class PipeFitting[InType] (
                          val next: Seq[ActorRef],
                          val context: ActorContext
                          ) {
  def |:(pipeSpec: PipeSpec) = new PipeFitting[pipeSpec.InType](
    next = List(pipeSpec.instantiate(context, next)),
    context = context
  )
  def ->:(sourceSpec: SourceSpec): Unit =
    sourceSpec.instantiate(context, next) ! Source.StartSending
}

