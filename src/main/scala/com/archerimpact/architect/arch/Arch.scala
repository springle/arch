package com.archerimpact.architect.arch

import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.{Level, Logger}
import com.archerimpact.architect.arch.pipes._
import com.archerimpact.architect.arch.sources.RMQSource
import org.slf4j.LoggerFactory

/*
               -/ohdy.
             `omsmNMho-`
         :o:yNo:`   .oymy:
       .omdd/`         -hhh
      `:oN+             `:d
     `dMd                 +:
   -.:Nm`                  /o-
  .hMMm:                   yh
    ymm`                   /+-
 .--shh        _____                 __
  :oo+/       /  _  \_______   ____ |  |__
             /  /_\  \_  __ \_/ ___\|  |  \
            /    |    \  | \/\  \___|   Y  \
            \____|__  /__|    \___  >___|  /
                    \/            \/     \/
*/

class ArchPipeline extends Pipeline {
  override def build(): Unit = new RMQSource ->:(
    new LoaderPipe
      |: new ParserPipe
      |: new ElasticPipe
//    |: new Neo4jPipe
//    |: new MatcherPipe
      \: this
    )
}

object Arch extends App {
  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.WARN)
  val system = ActorSystem("arch")
  val archPipeline = system.actorOf(Props(new ArchPipeline), "pipeline")
  APISource.startServer("0.0.0.0", 2724, system)
}

