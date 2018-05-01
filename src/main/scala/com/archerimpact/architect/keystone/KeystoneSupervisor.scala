package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import ch.qos.logback.classic.{Level, Logger}
import com.archerimpact.architect.keystone.pipes._
import com.archerimpact.architect.keystone.sources.{FakeNewsSource, OCTestingSource, RMQSource, SourceActor, FrontendAPISource}
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route

object KeystoneSupervisor {
  def props: Props = Props(new KeystoneSupervisor)
  final case object StartPipeline
  final case object IncReceived
  final case object IncLoaded
  final case object IncParsed
  final case object IncToNeo4j
  final case object IncToElastic
  final case object IncMatched
}

class KeystoneSupervisor extends Actor with ActorLogging {
  import KeystoneSupervisor._

  var received = 0
  var loaded = 0
  var parsed = 0
  var toNeo4j = 0
  var toElastic = 0
  var matched = 0

  override def preStart(): Unit = log.info("Keystone pipeline started.")
  override def postStop(): Unit = log.info("Keystone pipeline stopped.")

  def logStats(): Unit =
    log.info(
      s"received:$received, " +
      s"loaded:$loaded, " +
      s"parsed:$parsed, " +
      s"toNeo4j:$toNeo4j, " +
      s"toElastic:$toElastic, " +
      s"matched:$matched"
    )

  /* Pipe Actors */
  private val matcherPipe = context.actorOf(MatcherPipe.props(List[ActorRef]()), "matcher-pipe")
  private val elasticPipe = context.actorOf(ElasticPipe.props(List(matcherPipe)), "elastic-pipe")
  private val neo4jPipe = context.actorOf(Neo4jPipe.props(List(elasticPipe)), "neo4j-pipe")
  private val parserPipe = context.actorOf(ParserPipe.props(List(neo4jPipe)), "parser-pipe")
  private val loaderPipe = context.actorOf(LoaderPipe.props(List(parserPipe)), "loader-pipe")

  /* Source Actors */
  private val archerWorldSource = context.actorOf(RMQSource.props(loaderPipe), "archer-world-source")

  override def receive: PartialFunction[Any, Unit] = {
    case StartPipeline => archerWorldSource ! SourceActor.StartSending
    case IncReceived => received += 1; logStats()
    case IncLoaded => loaded += 1; logStats()
    case IncParsed => parsed += 1; logStats()
    case IncToNeo4j => toNeo4j += 1; logStats()
    case IncToElastic => toElastic += 1; logStats()
    case IncMatched => matched += 1; logStats()
  }
}

object Keystone extends App {
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.INFO)
  val system = ActorSystem("keystone-pipeline")
  val keystoneSupervisor = system.actorOf(KeystoneSupervisor.props, "keystone-supervisor")
  keystoneSupervisor ! KeystoneSupervisor.StartPipeline

  FrontendAPISource.startServer("localhost", 8080, system)

}

