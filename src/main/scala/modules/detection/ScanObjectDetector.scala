package modules.detection

import akka.actor.{Actor, ActorRef, Props}
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import configs.RootConfigs
import modules.bypass.ResolveDirRequest
import modules.scanner.{ScanReportDelegator, ScanRequest, Scanner}
import org.apache.commons.io.FilenameUtils
import services.mongo.MongoTemplate
import source.ScanObjectSource
import utils.UtilFunctions.{acceptsMagicNumber, deleteDirWithContents, using}

import java.nio.file.{Files, Path, Paths}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.EnumerationHasAsScala

case class ScanObject(path: Path, zipFileRef: Option[Path], replyTo: ActorRef, source: ScanObjectSource)

class ScanObjectDetector(config: RootConfigs, temps: Path) extends Actor with LazyLogging {

  private val mongoTemplate = MongoTemplate(config.mongo)

  private val dirResolver = context.system.actorSelection("user/DirectoryResolver")
  private val tempDirCleaner = context.system.actorSelection("user/TempDirCleaner")
  private val scanObjectReceiver = context.system.actorSelection("user/ScannerManager")
  private val scanReportDelegator =
    context.actorOf(Props(classOf[ScanReportDelegator], scanObjectReceiver))
  private val scanner = context.actorOf(Props(classOf[Scanner], mongoTemplate, scanReportDelegator), "Scanner")

  override def receive: Receive = {

    case dir: ScanObject if Files.isDirectory(dir.path) =>
      dirResolver ! ResolveDirRequest(dir, context.self)

    case file: ScanObject =>
      file match {
        case zip if acceptsMagicNumber(zip.path.toFile, config.magicNumbers.zip) => processZip(file)
        case exe if acceptsMagicNumber(exe.path.toFile, config.magicNumbers.executable) => processExecutable(file)
        case _ => ()
      }

  }

  private def processExecutable(scanObject: ScanObject) = {
    scanObjectReceiver ! scanObject
    scanner ! ScanRequest(scanObject.path, scanObject.zipFileRef, scanObject.replyTo, scanObject.source)
  }

  private def processZip(scanObject: ScanObject) = {
    val name = FilenameUtils.removeExtension(scanObject.path.getFileName.toString)
    val temp = Files.createTempDirectory(temps, name)

    tempDirCleaner ! temp

    unzip(scanObject.path, temp)

    val unzipped = temp.resolve(name)

    temp.toFile.listFiles().foreach { f =>
      val p = Paths.get(f.getAbsolutePath)
      if (p != unzipped) deleteDirWithContents(p)
    }

    context.self ! ScanObject(
      unzipped,
      scanObject.zipFileRef.getOrElse(scanObject.path).some,
      scanObject.replyTo,
      scanObject.source
    )

  }

  private def unzip(zipPath: Path, outputPath: Path): Unit =
    using(new ZipFile(zipPath.toFile)) { zipFile =>
      for (entry <- zipFile.entries.asScala) {
        val path = outputPath.resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(path)
        } else {
          Files.createDirectories(path.getParent)
          Files.copy(zipFile.getInputStream(entry), path)
        }
      }
    } {
      case e: Exception =>
        logger.error(s"Failed to unzip ${zipPath.getFileName}", e)
    }

}
