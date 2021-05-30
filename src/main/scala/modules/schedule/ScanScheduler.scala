package modules.schedule

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import configs.RootConfigs
import modules.detection.{ScanObject, ScanObjectDetector}
import source.Schedule

import java.nio.file.Path

case class ScheduledScanRequest(path: Path, cron: String, replyTo: ActorRef)

class ScanScheduler(config: RootConfigs, temps: Path) extends Actor {

  private val detector =
    context.actorOf(Props(classOf[ScanObjectDetector], config, temps), "ScanObjectDetector")

  override def receive: Receive = {
    case r: ScheduledScanRequest =>
      QuartzSchedulerExtension(context.system).createJobSchedule(
        "scanning-job",
        detector,
        ScanObject(r.path, None, r.replyTo, Schedule),
        cronExpression = r.cron
      )
  }

  override def postStop(): Unit = QuartzSchedulerExtension(context.system).deleteJobSchedule("scanning-job")
}
