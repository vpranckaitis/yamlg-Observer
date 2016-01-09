package lt.vpranckaitis.yamlg.observer.dto

case class LoginInfo(session: String, email: String, game: Option[BoardWithMoves])