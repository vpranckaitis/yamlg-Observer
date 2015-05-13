package lt.vpranckaitis.yamlg.observer.dto

import lt.vpranckaitis.yamlg.observer.repository.GameRepository

case class GameMetadata(id: GameRepository.GameId, started: Int, winner: Int, turns: Int = 0, botGame: Boolean, botId1: Int, botId2: Option[Int])