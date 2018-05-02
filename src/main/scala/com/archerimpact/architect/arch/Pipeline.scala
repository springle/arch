package com.archerimpact.architect.arch

import akka.actor.{Actor, ActorLogging}

trait Pipeline extends Actor with ActorLogging {

  def build(): Unit

  override def preStart(): Unit = build()

  def \:(pipeSpec: PipeSpec): PipeFitting[pipeSpec.InType] =
    new PipeFitting[pipeSpec.InType](List(pipeSpec.instantiate(context, List(self))), context)

  override def receive: Receive = {
    case _ => None
  }
}
