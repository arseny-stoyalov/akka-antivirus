package utils

import model.MagicNumber
import org.apache.commons.io.filefilter.MagicNumberFileFilter
import utils.implicits.Extensions.ByteStringEx

import java.io.{Closeable, File}
import java.nio.file.{Files, Path}
import java.util.Comparator
import scala.jdk.StreamConverters.StreamHasToScala

object UtilFunctions {

  def md5(bytes: Array[Byte]): String = {
    import java.math.BigInteger
    import java.security.MessageDigest
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(bytes)
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }

  def using[T <: Closeable, U](resource: T)(block: T => U)(logError: PartialFunction[Throwable, U]): U =
    try {
      block(resource)
    } catch logError
    finally {
      if (resource != null) {
        resource.close()
      }
    }

  def acceptsMagicNumber(file: File, magicNumbers: List[MagicNumber]) =
    magicNumbers.exists(mn => new MagicNumberFileFilter(mn.byteString.parseBytes, mn.offset).accept(file))

  def deleteDirWithContents(path: Path) =
    Files.walk(path).sorted(Comparator.reverseOrder[Path]()).toScala(List).foreach(Files.delete)

}
