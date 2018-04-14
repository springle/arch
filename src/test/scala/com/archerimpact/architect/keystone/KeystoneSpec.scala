package com.archerimpact.architect.keystone

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class KeystoneSpec() extends TestKit(ActorSystem("KeystoneSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Loader actor" must {

    "send a ParseShipment message" in {
      val probe = TestProbe()
      val loader = system.actorOf(Loader.props(probe.ref), "loader")
      val dummyDataSource = system.actorOf(DummyDataSource.props, "dummy-data-source")
      loader ! Loader.StartLoading(dummyDataSource)
      probe.expectMsgType[Parser.ParseShipment]
    }

  }
}
