import java.io.Serializable

case object commons {

  sealed trait RequestToService

  case class SingleScanRequest(path: String) extends RequestToService with Serializable
  case object StopSingleScanRequest extends RequestToService with Serializable
  case class MonitoringRequest(path: String) extends RequestToService with Serializable
  case object StopMonitoringRequest extends RequestToService with Serializable
  case class ScheduleRequest(path: String, cron: String) extends RequestToService with Serializable
  case object StopScheduleRequest extends RequestToService with Serializable
  case object StopServiceRequest extends RequestToService with Serializable

  case class StoppedScanResponse(source: String) extends Serializable

  case class ServiceAnswer(
    path: String,
    scanning: Boolean,
    hasMalware: Option[Boolean],
    malwareName: Option[String],
    source: String
  ) extends Serializable {
    override def toString: String =
      s"$path ${malwareResultString.getOrElse("")} | $source | ${if (scanning) "scanning" else ""}"

    private def malwareResultString: Option[String] =
      for {
        h <- hasMalware
        m <- malwareName
      } yield if (h) m else "clear"
  }

}
