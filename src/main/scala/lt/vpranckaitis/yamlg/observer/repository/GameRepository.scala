package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board

object GameRepository {
  
  type GameId = Long
  
}

trait GameRepository {
  
  type GameId = GameRepository.GameId
  
  def startGame(started: Int): GameId
  def getGameMetadata(gameId: GameId): GameMetadata
  def getMoves(gameId: GameId): Seq[Board]
  def saveMove(gameId: GameId, board: Board)
  def saveGame(gameId: GameId, started: Int, winner: Int, boards: Seq[Board])
  def finishGame(gameId: GameId, winner: Int)

}