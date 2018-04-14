package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.pipeline.DataSource.StartSending
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

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

  def setupConnection: ActorRef = {
    val factory = new ConnectionFactory()
    factory.setUsername(username)
    factory.setPassword(password)
    factory.setHost(host)
    factory.setPort(port)
    context.actorOf(ConnectionActor.props(factory), "rmq-connection")
  }

  override def receive = {

    case StartSending(target: ActorRef) =>
      def setupSubscriber(channel: Channel, self: ActorRef) = {
        val queue = channel.queueDeclare().getQueue
        channel.queueBind(queue, exchange, "")
        val consumer = new DefaultConsumer(channel) {
          override def handleDelivery(consumerTag: String,
                                      envelope: Envelope,
                                      properties: BasicProperties,
                                      body: Array[Byte]) = {
            target ! Loader.LoadShipment(new DummyShipment(dataSource = "rmq", data = fromBytes(body)))
          }
        }
        channel.basicConsume(queue, true, consumer)
      }
      setupConnection ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))
      log.info(s"Subscribing to ${exchange}")
  }

}

/* ------------------------------ */
/* A dummy DataSource for testing */
/* ------------------------------ */

object DummyDataSource {
  def props: Props = Props(new DummyDataSource)
}

class DummyDataSource extends DataSource {
  override def receive = {
    case StartSending(target: ActorRef) =>
      for (_ <- 0 to 10)
        target ! Loader.LoadShipment(new DummyShipment())
  }
}