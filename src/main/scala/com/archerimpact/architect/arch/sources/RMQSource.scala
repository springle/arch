package com.archerimpact.architect.arch.sources

import akka.actor.{ActorContext, ActorRef}
import com.archerimpact.architect.arch.SourceSpec
import com.archerimpact.architect.arch.shipments.UrlShipment
import com.newmotion.akka.rabbitmq._

class RMQSource extends SourceSpec {

  val username: String = scala.util.Properties.envOrElse("RMQ_USERNAME", "architect")
  val password: String = scala.util.Properties.envOrElse("RMQ_PASSWORD", "gotpublicdata")
  val host: String = scala.util.Properties.envOrElse("RMQ_HOST", "localhost")
  val port: Int = scala.util.Properties.envOrElse("RMQ_PORT", "5672").toInt
  val exchange: String = scala.util.Properties.envOrElse("RMQ_EXCHANGE", "sources")

  private def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

  private def setupSubscriber(send: OutType => Unit) = {
    (channel: Channel, _: ActorRef) => {
      val queue = channel.queueDeclare().getQueue
      channel.exchangeDeclare(exchange, "fanout")
      channel.queueBind(queue, exchange, "")
      channel.basicConsume(queue, true, setupConsumer(channel, send))
    }
  }

  private def setupConsumer(channel: Channel, send: OutType => Unit) = {
    new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String,
                                  envelope: Envelope,
                                  properties: BasicProperties,
                                  body: Array[Byte]): Unit = {
        send(new UrlShipment(fromBytes(body)))
      }
    }
  }

  private def setupConnection(context: ActorContext): ActorRef = {
    val factory = new ConnectionFactory()
    factory.setUsername(username)
    factory.setPassword(password)
    factory.setHost(host)
    factory.setPort(port)
    context.actorOf(ConnectionActor.props(factory), "rmq-connection")
  }

  override type OutType = UrlShipment
  override def run(send: OutType => Unit, context: ActorContext): Unit =
    setupConnection(context) ! CreateChannel(ChannelActor.props(setupSubscriber(send)), Some("subscriber"))
}
