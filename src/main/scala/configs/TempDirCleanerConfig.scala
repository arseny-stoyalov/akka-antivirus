package configs

import scala.concurrent.duration.FiniteDuration

case class TempDirCleanerConfig(ttl: FiniteDuration, delay: FiniteDuration)
