package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorContext, ActorRef}
import com.archerimpact.architect.keystone.SourceSpec
import com.archerimpact.architect.keystone.shipments.UrlShipment
import com.newmotion.akka.rabbitmq._

class RMQSource(
                val username: String = "architect",
                val password: String = "gotpublicdata",
                val host: String = "localhost",
                val port: Int = 5672,
                val exchange: String = "sources"
              ) extends SourceSpec {

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
