package com.archerimpact.architect.pipeline

import akka.actor.{ Actor, ActorRef, Props }
import com.newmotion.akka.rabbitmq._

object DataSource {
  final case class StartSending(target: ActorRef)
}

trait DataSource extends Actor {
  import DataSource._
  override def receive = {
    case StartSending => ???
  }
}

/* A DataSource for consuming from RabbitMQ */

class RabbitDataSource extends DataSource {
  val factory = new ConnectionFactory()
  val connection = factory.newConnection()
}

/* A dummy DataSource for testing */

object DummyDataSource {
  def props: Props = Props(new DummyDataSource)
}

class DummyDataSource extends DataSource {
  import DataSource._
  override def receive = {
    case StartSending(target: ActorRef) =>
      for (_ <- 0 to 10)
        target ! LoaderSupervisor.LoadShipment(new DummyShipment)
  }
}