package lt.vpranckaitis.yamlg.observer.dto

import spray.json.DefaultJsonProtocol

object ExtendedJsonProtocol extends DefaultJsonProtocol {
  implicit val BoardFormat = jsonFormat2(Board)
  implicit val BoardWithMovesFormat = jsonFormat3(BoardWithMoves)
  implicit val GameMetadataFormat = jsonFormat3(GameMetadata)
  implicit val GameSetupFormat = jsonFormat1(GameSetup)
  implicit val GameFormat = jsonFormat4(Game)
}