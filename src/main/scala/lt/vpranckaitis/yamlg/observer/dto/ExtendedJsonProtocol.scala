package lt.vpranckaitis.yamlg.observer.dto

import spray.json.DefaultJsonProtocol

object ExtendedJsonProtocol extends DefaultJsonProtocol {
  implicit val BoardFormat = jsonFormat2(Board)
  implicit val BoardWithMovesFormat = jsonFormat4(BoardWithMoves)
  implicit val GameMetadataFormat = jsonFormat4(GameMetadata)
  implicit val GameSetupFormat = jsonFormat1(GameSetup)
  implicit val GameFormat = jsonFormat4(Game)
  implicit val GadeIdsFormat = jsonFormat1(GameIds)
}