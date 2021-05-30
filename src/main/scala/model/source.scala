package object source {

  sealed trait ScanObjectSource

  case object Monitor extends ScanObjectSource
  case object Schedule extends ScanObjectSource
  case object SingleScan extends ScanObjectSource

}
