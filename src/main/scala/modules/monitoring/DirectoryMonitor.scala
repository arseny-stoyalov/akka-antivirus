package modules.monitoring

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import com.typesafe.scalalogging.LazyLogging
import configs.{DirectoryMonitorConfig, RootConfigs}
import modules.detection.{ScanObject, ScanObjectDetector}
import source.Monitor

import java.nio.file.{Files, Path}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.DurationInt
import scala.jdk.StreamConverters.StreamHasToScala

case class StartMonitoringProcess(path: Path, replyTo: ActorRef)
case class MonitoringTick(path: Path, replyTo: ActorRef)

class DirectoryMonitor(config: RootConfigs, temps: Path) extends Actor with LazyLogging {

  private val detector =
    context.actorOf(Props(classOf[ScanObjectDetector], config, temps), "ScanObjectDetector")
  private val dirsMonitored: MutableMap[Path, Set[Path]] = MutableMap.empty[Path, Set[Path]]
  private var scheduleCancellable: Cancellable = Cancellable.alreadyCancelled

  override def receive: Receive = {
    case s: StartMonitoringProcess =>
      scheduleCancellable.cancel()
      logger.debug(s"Started monitoring ${s.path}")
      dirsMonitored.addOne(s.path -> Set.empty[Path])
      scheduleCancellable = context.system.scheduler.scheduleWithFixedDelay(
        0.seconds,
        config.directoryMonitor.scheduleDelay,
        context.self,
        MonitoringTick(s.path, s.replyTo)
      )(context.system.dispatcher)

    case t: MonitoringTick =>
      val p = t.path
      dirsMonitored.get(p) match {
        case Some(previous) =>
          val current = Files.walk(p).toScala(Set)
          val deleted = previous.diff(current)
          val added = current.diff(previous)
          dirsMonitored.put(p, previous.removedAll(deleted) ++ added)
          added.foreach(a => detector ! ScanObject(a, None, t.replyTo, Monitor))
        case None => ()
      }
  }

  override def postStop(): Unit = scheduleCancellable.cancel()
}
