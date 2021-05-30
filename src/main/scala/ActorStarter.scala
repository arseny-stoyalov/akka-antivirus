import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import configs.{MagicNumberConfig, RootConfigs}
import modules.bypass.DirectoryResolver
import modules.detection.{ScanObject, ScanObjectDetector, TempDirCleaner}
import modules.monitoring.{DirectoryMonitor, StartMonitoringProcess}
import modules.scanner.{ScanResponse, Scanner}
import modules.schedule.ScanScheduler
import services.mongo.MongoTemplate

import java.nio.file.{Files, Path, Paths}

object ActorStarter {

  class ScannerManager(configs: RootConfigs) extends Actor {

    val mongoTemplate: MongoTemplate = MongoTemplate(configs.mongo)

    val temps: Path =
      Files.createTempDirectory(Paths.get("/Users/a1/Downloads/study"), ".")

    private val detector =
      context.system.actorOf(Props(classOf[ScanObjectDetector], configs.magicNumbers, temps), "ScanObjectDetector")
    private val monitor =
      context.system.actorOf(Props(classOf[DirectoryMonitor], configs.directoryMonitor), "DirectoryMonitor")

    private val actors =
      detector :: monitor :: List(
        context.system.actorOf(Props(classOf[Scanner], mongoTemplate), "Scanner"),
        context.system.actorOf(Props(classOf[DirectoryResolver]), "DirectoryResolver"),
        context.system.actorOf(Props(classOf[TempDirCleaner], configs.tempDirCleaner), "TempDirCleaner"),
        context.system.actorOf(Props(classOf[ScanScheduler], configs.magicNumbers), "ScanScheduler")
      )

    override def preStart(): Unit = {
      println("Initial message sent")
//      detector ! ScanObject(Paths.get("/Users/a1/Downloads/study/executables"), None)
      monitor ! StartMonitoringProcess(Paths.get("/Users/a1/Downloads/study/executables"))
    }

    override def receive: Receive = {
      case r: ScanResponse =>
        println(r)
    }

  }

  def setUpActors(configs: RootConfigs)(implicit system: ActorSystem): Unit =
    system.actorOf(Props(classOf[ScannerManager], configs), "ScannerManager") //todo: refactor
//    system.scheduler.scheduleOnce(5.millis)(initialActor ! "Stop")(system.dispatcher)

}
