package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.archerimpact.architect.keystone.shipments.{GraphShipment, UrlShipment}

object FrontendAPISource extends HttpApp {
  override def routes: Route =
    path("architect") {
      get {
        //do other stuff
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>hey hey hey</h1>"))
      }
    }
}
