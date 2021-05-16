import akka.actor.{Actor, ActorSystem, Props}
import modules.scanner.{ScanRequest, ScanResponse, Scanner}
import modules.storage.AntivirusStorage
import services.mongo.MongoTemplate

import scala.concurrent.duration.DurationInt

object ActorStarter {

  class InitialActor(mongoTemplate: MongoTemplate) extends Actor {

    private val scanner = context.actorOf(Props(classOf[Scanner], mongoTemplate), "Scanner")
    //context.system.actorSelection("user/Scanner")

    override def preStart(): Unit = {
      println("Initial message sent")
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main1.exe", context.self)
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main2.exe", context.self)
      scanner ! ScanRequest("/Users/a1/Downloads/study/executables/main3.exe", context.self)
    }

    var count = 0

    override def receive: Receive = {
      case r: ScanResponse =>
        println(r)
      case _: String => context.stop(scanner)
    }

  }

  def setUpActors(mongoTemplate: MongoTemplate)(implicit system: ActorSystem): Unit = {

    system.actorOf(Props(classOf[AntivirusStorage], mongoTemplate), "AntivirusStorage")
    val initialActor = system.actorOf(Props(classOf[InitialActor], mongoTemplate), "InitialActor")

    system.scheduler.scheduleOnce(5.millis)(initialActor ! "Stop")(system.dispatcher)

  }

}
