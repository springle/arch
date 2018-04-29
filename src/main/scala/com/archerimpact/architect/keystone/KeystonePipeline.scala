package com.archerimpact.architect.keystone

import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.{Level, Logger}
import com.archerimpact.architect.keystone.pipes._
import com.archerimpact.architect.keystone.sources.RMQSource
import org.slf4j.LoggerFactory

class KeystonePipeline extends Pipeline {
  override def build(): Unit = {
    new RMQSource() ->: (new LoaderPipe |: new ParserPipe |:
      new Neo4jPipe |: new ElasticPipe |: new MatcherPipe \: this)
  }
}

object Keystone extends App {
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.WARN)
  val system = ActorSystem("keystone-pipeline")
  val keystonePipeline = system.actorOf(Props(new KeystonePipeline), "keystone-pipeline")
}

