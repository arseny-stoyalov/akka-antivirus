package utils.implicits

import org.bson.codecs.{DecoderContext, EncoderContext}

object Global {

  implicit val bsonDecoderContext: DecoderContext = DecoderContext.builder.build

  implicit val bsonEncoderContext: EncoderContext = EncoderContext.builder().build()

}
