package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.keystone.DataSource.StartSending
import com.newmotion.akka.rabbitmq._

object DataSource {
  final case class StartSending(target: ActorRef)
}

trait DataSource extends Actor with ActorLogging

/* ---------------------------------------- */
/* A DataSource for consuming from RabbitMQ */
/* ---------------------------------------- */

object RMQDataSource {
  def props(
             username: String = "architect",
             password: String = "gotpublicdata",
             host: String = "localhost",
             port: Int = 5672,
             exchange: String = "amq.fanout"
           ): Props = Props(new RMQDataSource(username, password, host, port, exchange))
}

class RMQDataSource(
                     val username: String,
                     val password: String,
                     val host: String,
                     val port: Int,
                     val exchange: String
                   ) extends DataSource {

  override def receive: PartialFunction[Any, Unit] = {

    case StartSending(target: ActorRef) =>
      def setupSubscriber(channel: Channel, self: ActorRef) = {
        val queue = channel.queueDeclare().getQueue
        channel.queueBind(queue, exchange, "")
        channel.basicConsume(queue, true, setupConsumer(channel, target))
      }

      setupConnection ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))
  }

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

  private def setupConsumer(channel: Channel, target: ActorRef) = {
    new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String,
                                  envelope: Envelope,
                                  properties: BasicProperties,
                                  body: Array[Byte]): Unit = {
        val dataFormat = properties.getHeaders.get("dataFormat").toString
        target ! Loader.PackageShipment(url=fromBytes(body), dataFormat=dataFormat)
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

object DummyDataSource {
  def props: Props = Props(new DummyDataSource)
}

class DummyDataSource extends DataSource {
  override def receive: PartialFunction[Any, Unit] = {
    case StartSending(target: ActorRef) =>
      for (_ <- 0 to 10)
        target ! Loader.PackageShipment(url="dum://my.source", dataFormat="dummy")
  }
}