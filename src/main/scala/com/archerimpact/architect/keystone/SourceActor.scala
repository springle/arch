package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.keystone.SourceActor.StartSending
import com.newmotion.akka.rabbitmq._

object SourceActor {
  final case class StartSending(target: ActorRef)
}

trait SourceActor extends Actor with ActorLogging

/* ---------------------------------------- */
/* A DataSource for consuming from RabbitMQ */
/* ---------------------------------------- */

object RMQSourceActor {
  def props(
             username: String = "architect",
             password: String = "gotpublicdata",
             host: String = "localhost",
             port: Int = 5672,
             exchange: String = "sources"
           ): Props = Props(new RMQSourceActor(username, password, host, port, exchange))
}

class RMQSourceActor(
                     val username: String,
                     val password: String,
                     val host: String,
                     val port: Int,
                     val exchange: String
                   ) extends SourceActor {

  override def receive: Receive = {
    case StartSending(target: ActorRef) =>
      setupConnection ! CreateChannel(ChannelActor.props(setupSubscriber(target)), Some("subscriber"))
  }

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

  private def setupSubscriber(target: ActorRef): (Channel, ActorRef) => Any = {
    (channel: Channel, _: ActorRef) => {
        val queue = channel.queueDeclare().getQueue
        channel.exchangeDeclare(exchange, "fanout")
        channel.queueBind(queue, exchange, "")
        channel.basicConsume(queue, true, setupConsumer(channel, target))
    }
  }

  private def setupConsumer(channel: Channel, target: ActorRef) = {
    new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String,
                                  envelope: Envelope,
                                  properties: BasicProperties,
                                  body: Array[Byte]): Unit = {
        val dataFormat = properties.getHeaders.get("dataFormat").toString
        context.parent ! KeystoneSupervisor.IncReceived
        target ! LoaderPipe.PackageShipment(url=fromBytes(body), dataFormat=dataFormat)
      }
    }
  }

  private def setupConnection: ActorRef = {
    val factory = new ConnectionFactory()
    factory.setUsername(username)
    factory.setPassword(password)
    factory.setHost(host)
    factory.setPort(port)
    context.actorOf(ConnectionActor.props(factory), "rmq-connection")
  }

}

/* ------------------------------ */
/* A dummy DataSource for testing */
/* ------------------------------ */

object DummySourceActor {
  def props: Props = Props(new DummySourceActor)
}

class DummySourceActor extends SourceActor {
  override def receive: Receive = {
    case StartSending(target: ActorRef) =>
      for (_ <- 0 to 10)
        target ! LoaderPipe.PackageShipment(url="dum://my.source", dataFormat="dummy")
  }
}