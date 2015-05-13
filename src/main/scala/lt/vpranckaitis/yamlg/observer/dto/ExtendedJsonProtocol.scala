package lt.vpranckaitis.yamlg.observer.dto

import spray.json.DefaultJsonProtocol

object ExtendedJsonProtocol extends DefaultJsonProtocol {
  implicit val BoardFormat = jsonFormat2(Board)
  implicit val BoardWithMovesFormat = jsonFormat4(BoardWithMoves)
  implicit val GameMetadataFormat = jsonFormat7(GameMetadata)
  implicit val GameSetupFormat = jsonFormat3(GameSetup)
  implicit val GameFormat = jsonFormat4(Game)
  implicit val GadeIdsFormat = jsonFormat1(GameIds)
  implicit val LoginInfoFormat = jsonFormat3(LoginInfo)
  implicit val AccessTokenFormat = jsonFormat1(AccessToken) 
  implicit val GoogleAuthFormat = jsonFormat1(GoogleAuth) 
  implicit val StatisticsFormat = jsonFormat3(Statistics) 
  implicit val GameMetadatasFormat = jsonFormat3(GameMetadatas) 
  implicit val BotFormat = jsonFormat6(Bot) 
  implicit val BotGameSetupFormat = jsonFormat3(BotGameSetup) 
}