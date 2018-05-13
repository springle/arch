package com.archerimpact.architect.arch

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

  /* Perform the pipe's function/transformation */
  def flow(input: InType): OutType

  /* Instructions to create the pipe */
  def props(next: Seq[ActorRef]): Props =
    Props(new Pipe[InType, OutType](next, flow))

  /* Install the pipe in the actor system */
  def instantiate(context: ActorContext, next: Seq[ActorRef]): ActorRef =
    context.actorOf(props(next), typeName(this))
}

class PipeFitting[InType] (
                          val next: Seq[ActorRef],
                          val context: ActorContext
                          ) {

  /* Chain two pipes together */
  def |:(pipeSpec: PipeSpec) =
    new PipeFitting[pipeSpec.InType] (
      next = List(pipeSpec.instantiate(context, next)),
      context = context
    )

  /* Connect a source to a pipe */
  def ->:(sourceSpec: SourceSpec): Unit =
    sourceSpec.instantiate(context, next) ! Source.StartSending
}

