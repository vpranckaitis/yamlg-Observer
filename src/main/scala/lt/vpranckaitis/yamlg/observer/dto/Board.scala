package lt.vpranckaitis.yamlg.observer.dto

import lt.vpranckaitis.yamlg.observer.repository.GameRepository

case class Board(board: String, gameId: Option[GameRepository.GameId] = None) {
  override def toString = board;
}