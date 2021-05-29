package modules.schedule

import akka.actor.{Actor, Props}
import configs.MagicNumberConfig
import modules.detection.{ScanObject, ScanObjectDetector}

import java.nio.file.{Files, Path}
import scala.concurrent.duration.DurationInt

case class ScheduleRequest(path: Path, initialDelay: Int, delay: Int)

class ScanScheduler(config: MagicNumberConfig) extends Actor {

  override def receive: Receive = {
    case r: ScheduleRequest =>
      val detectorDirPath = Files.createTempDirectory(r.path.getParent, ".")
      context.system.scheduler.scheduleWithFixedDelay(
        r.initialDelay.minutes,
        r.delay.minutes,
        context.actorOf(Props(classOf[ScanObjectDetector], config, detectorDirPath)),
        ScanObject(r.path, None)
      )(context.dispatcher)
  }

}
