package modules.detection

import akka.actor.{Actor, ActorRef}
import configs.MagicNumberConfig
import modules.bypass.ResolveDirRequest
import modules.scanner.ScanRequest
import org.apache.commons.io.FilenameUtils
import utils.UtilFunctions.{acceptsMagicNumber, deleteDirWithContents, using}
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging

import java.nio.file.{Files, Path, Paths}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.EnumerationHasAsScala

case class ScanObject(path: Path, zipFileRef: Option[Path])

class ScanObjectDetector(config: MagicNumberConfig, temps: Path) extends Actor with LazyLogging {

  private val dirResolver = context.system.actorSelection("user/DirectoryResolver")
  private val tempDirCleaner = context.system.actorSelection("user/TempDirCleaner")
  private val scanner = context.system.actorSelection("user/Scanner")

  override def receive: Receive = {

    case dir: ScanObject if Files.isDirectory(dir.path) =>
      dirResolver ! ResolveDirRequest(dir, context.self)

    case file: ScanObject =>
      file match {
        case zip if acceptsMagicNumber(zip.path.toFile, config.zip) => processZip(file)
        case exe if acceptsMagicNumber(exe.path.toFile, config.executable) => processExecutable(file)
        case _ => ()
      }

  }

  override def postStop(): Unit =
    deleteDirWithContents(temps)

  def processExecutable(scanObject: ScanObject) =
    scanner ! ScanRequest(scanObject.path, scanObject.zipFileRef)

  def processZip(scanObject: ScanObject) = {
    val name = FilenameUtils.removeExtension(scanObject.path.getFileName.toString)
    val temp = Files.createTempDirectory(temps, name)

    tempDirCleaner ! temp

    unzip(scanObject.path, temp)

    val unzipped = temp.resolve(name)

    temp.toFile.listFiles().foreach { f =>
      val p = Paths.get(f.getAbsolutePath)
      if (p != unzipped) deleteDirWithContents(p)
    }

    context.self ! ScanObject(unzipped, scanObject.zipFileRef.getOrElse(scanObject.path).some)

  }

  def unzip(zipPath: Path, outputPath: Path): Unit =
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
