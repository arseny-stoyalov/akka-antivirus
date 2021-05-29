package configs

case class RootConfigs(mongo: MongoConfigs, magicNumbers: MagicNumberConfig, tempDirCleaner: TempDirCleanerConfig)
