import akka.actor.{Actor, ActorSystem, Props}
import commons.{
  MonitoringRequest,
  ScheduleRequest,
  ServiceAnswer,
  SingleScanRequest,
  StopMonitoringRequest,
  StopScheduleRequest,
  StopServiceRequest,
  StopSingleScanRequest,
  StoppedScanResponse
}
import configs.RootConfigs
import modules.bypass.DirectoryResolver
import modules.detection.{ScanObject, ScanObjectDetector, TempDirCleaner}
import modules.monitoring.{DirectoryMonitor, StartMonitoringProcess}
import modules.scanner.ScanResponse
import modules.schedule.{ScanScheduler, ScheduledScanRequest}
import services.mongo.MongoTemplate
import source.{Monitor, Schedule, SingleScan}
import utils.UtilFunctions.deleteDirWithContents

import java.nio.file.{Files, Path, Paths}

object ActorStarter {

  class ScannerManager(configs: RootConfigs) extends Actor {

    val mongoTemplate: MongoTemplate = MongoTemplate(configs.mongo)

    val temps: Path =
      Files.createTempDirectory(Paths.get(configs.tempDirPath), ".")

    private var detector =
      context.actorOf(Props(classOf[ScanObjectDetector], configs, temps))

    private var monitor =
      context.system.actorOf(Props(classOf[DirectoryMonitor], configs, temps))

    private var scheduler =
      context.system.actorOf(Props(classOf[ScanScheduler], configs, temps))

    private val actors =
      detector :: monitor :: scheduler :: List(
        context.system.actorOf(Props(classOf[DirectoryResolver]), "DirectoryResolver"),
        context.system.actorOf(Props(classOf[TempDirCleaner], configs.tempDirCleaner), "TempDirCleaner")
      )

    override def receive: Receive = {
      case r: ScanResponse =>
        r.replyTo ! ServiceAnswer(
          r.path.toString,
          scanning = false,
          Some(r.matchedMalware),
          Some(r.malwareName),
          r.source.toString
        )
      case StopServiceRequest => stopService
      case o: ScanObject =>
        val path = o.zipFileRef.getOrElse(o.path)
        o.replyTo ! ServiceAnswer(path.toString, scanning = true, None, None, o.source.toString)

      case r: SingleScanRequest => detector ! ScanObject(Paths.get(r.path), None, sender(), SingleScan)
      case StopSingleScanRequest =>
        context.stop(detector)
        detector = context.actorOf(Props(classOf[ScanObjectDetector], configs, temps))
        sender() ! StoppedScanResponse(SingleScan.toString)

      case r: ScheduleRequest => scheduler ! ScheduledScanRequest(Paths.get(r.path), r.cron, sender())
      case StopScheduleRequest =>
        context.stop(scheduler)
        scheduler = context.system.actorOf(Props(classOf[ScanScheduler], configs, temps))
        sender() ! StoppedScanResponse(Schedule.toString)

      case r: MonitoringRequest => monitor ! StartMonitoringProcess(Paths.get(r.path), sender())
      case StopMonitoringRequest =>
        context.stop(monitor)
        monitor = context.system.actorOf(Props(classOf[DirectoryMonitor], configs, temps))
        sender() ! StoppedScanResponse(Monitor.toString)

    }

    def stopService = {
      actors.foreach(context.stop)
      deleteDirWithContents(temps)
      context.system.terminate()
    }

  }

  def setUpActors(configs: RootConfigs)(implicit system: ActorSystem): Unit =
    system.actorOf(Props(classOf[ScannerManager], configs), "ScannerManager")

}
