package com.github.diegopacheco.scala.playground.akka.cluster.routers

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Actor
import com.github.diegopacheco.scala.playground.akka.cluster.dispatcher.SimplePrinterActor
import akka.actor.Props
import akka.routing.FromConfig
import akka.actor.ActorRef
import akka.actor.SupervisorStrategy
import akka.routing.RoundRobinPool
import akka.actor.OneForOneStrategy

class SimpleRRActor extends Actor {
  def receive = {
    case s: String =>
      println("Print: " + s + " - " + self.path + " - " + context.dispatcher)
  }
}

object SimpleRRApp extends App {

  def bootup(port: String, dispatcher: String): ActorSystem = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString(s"""
                        akka.actor.deployment {
                          /router1 {
                              router = round-robin-pool
                              nr-of-instances = 5
                          }
                        }
                    """)).withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", config)
    system
  }

  val system = bootup("2551", "compute")
  val router1: ActorRef = system.actorOf(FromConfig.props(Props[SimpleRRActor]), "router1")
  for (i <- 1 to 20) router1 ! s"Hi there? $i "

  val escalator = OneForOneStrategy() {
    case e => router1 ! e; SupervisorStrategy.Escalate
  }
  val router2 = system.actorOf(RoundRobinPool(1, supervisorStrategy = escalator).props( routeeProps = Props[SimpleRRActor]))
  for (i <- 1 to 10) router2 ! s"Hi there 2 ? $i "
  
}
  
