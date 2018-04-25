package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.UrlShipment
import com.newmotion.akka.rabbitmq._

object RMQSource {
  def props(
             target: ActorRef,
             username: String = "architect",
             password: String = "gotpublicdata",
             host: String = "localhost",
             port: Int = 5672,
             exchange: String = "sources"
           ): Props = Props(new RMQSource(target, username, password, host, port, exchange))
}

class RMQSource(
                target: ActorRef,
                val username: String,
                val password: String,
                val host: String,
                val port: Int,
                val exchange: String
              ) extends SourceActor(target) {

  override def startSending(): Unit =
    setupConnection ! CreateChannel(ChannelActor.props(setupSubscriber(target)), Some("subscriber"))

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
        sendShipment(new UrlShipment(fromBytes(body)))
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
