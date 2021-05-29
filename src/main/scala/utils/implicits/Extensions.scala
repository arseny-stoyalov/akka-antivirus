package utils.implicits

object Extensions {

  implicit class ByteStringEx(byteString: String) {
    def parseBytes: Array[Byte] = byteString.split(" ").map(Integer.parseInt(_, 16).toByte)
  }

}
