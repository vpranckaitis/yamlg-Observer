package lt.vpranckaitis.yamlg.observer.dto

import lt.vpranckaitis.yamlg.observer.repository.GameRepository

case class BoardWithMoves(board: String, moves: Array[Seq[Int]], gameId: Option[GameRepository.GameId] = None, finished: Int = 0)