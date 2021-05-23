import model.MalwareObject
import modules.scanner.Scanner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import utils.UtilFunctions.md5

class ScannerTest extends AnyFunSuite with Matchers {

  val malwareBytes: Array[Byte] = Array(5, 4, 3, 2, 1)
  val mockMalware: MalwareObject = MalwareObject("", "mock", 5, malwareBytes.take(3), md5(malwareBytes), 3, 3)

  val toCheck: Array[Byte] = Array(0, 1, 5, 4, 3, 5, 4, 3, 2, 1, 1, 1, 2, 2, 3, 0)

  test("basic scanner test") {

    Scanner.isInfected(toCheck, mockMalware) shouldBe true

  }

}
