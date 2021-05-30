package modules.detection

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import configs.TempDirCleanerConfig
import utils.UtilFunctions.deleteDirWithContents

import java.nio.file.Path
import java.util.Date
import scala.collection.mutable.{Set => MutableSet}
import scala.concurrent.duration.DurationInt

class TempDirCleaner(config: TempDirCleanerConfig) extends Actor with LazyLogging {

  val tempDirs: MutableSet[(Path, Date)] = MutableSet.empty

  override def preStart(): Unit = {
    logger.info("Temp directories cleaner started")
    context.system.scheduler.scheduleWithFixedDelay(0.seconds, config.delay) { () =>
      val toDelete = tempDirs.filter {
        case (_, created) => (new Date().getTime - created.getTime) >= config.ttl.toMillis
      }
      logger.info(s"Removing [${toDelete.size}] temp directories")
      tempDirs --= toDelete
      toDelete.foreach {
        case (path, _) => deleteDirWithContents(path)
      }
    }(context.system.dispatcher)
  }

  override def receive: Receive = {
    case newDir: Path => tempDirs.addOne(newDir -> new Date())

  }

}
