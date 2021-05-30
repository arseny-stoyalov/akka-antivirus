package modules.monitoring

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import configs.DirectoryMonitorConfig
import modules.detection.ScanObject

import java.nio.file.{Files, Path}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.DurationInt
import scala.jdk.StreamConverters.StreamHasToScala

case class StartMonitoringProcess(path: Path)

class DirectoryMonitor(config: DirectoryMonitorConfig) extends Actor with LazyLogging {

  private val detector = context.system.actorSelection("user/ScanObjectDetector")
  private val dirsMonitored: MutableMap[Path, Set[Path]] = MutableMap.empty[Path, Set[Path]]

  override def receive: Receive = {
    case s: StartMonitoringProcess =>
      startMonitorProcess(s.path)

    case p: Path =>
      dirsMonitored.get(p) match {
        case Some(previous) =>
          val current = Files.walk(p).toScala(Set)
          val deleted = previous.diff(current)
          val added = current.diff(previous)
          dirsMonitored.put(p, previous.removedAll(deleted) ++ added)
          added.foreach(a => detector ! ScanObject(a, None))
        case None => ()
      }
  }

  def startMonitorProcess(path: Path) = {
    logger.debug(s"Started monitoring $path")
    dirsMonitored.addOne(path -> Set.empty[Path])
    context.system.scheduler.scheduleWithFixedDelay(
      0.seconds,
      config.scheduleDelay,
      context.self,
      path
    )(context.system.dispatcher)
  }

}
