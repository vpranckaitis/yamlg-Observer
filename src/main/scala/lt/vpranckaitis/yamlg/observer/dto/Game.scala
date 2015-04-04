package lt.vpranckaitis.yamlg.observer.dto

import lt.vpranckaitis.yamlg.observer.repository.GameRepository

case class Game(gameId: GameRepository.GameId, started: Int, winner: Int, boards: List[Board])