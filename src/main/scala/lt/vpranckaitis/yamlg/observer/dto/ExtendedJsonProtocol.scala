package lt.vpranckaitis.yamlg.observer.dto

import spray.json.DefaultJsonProtocol

object ExtendedJsonProtocol extends DefaultJsonProtocol {
  implicit val BoardFormat = jsonFormat1(Board)
  implicit val BoardWithMovesFormat = jsonFormat2(BoardWithMoves)
}