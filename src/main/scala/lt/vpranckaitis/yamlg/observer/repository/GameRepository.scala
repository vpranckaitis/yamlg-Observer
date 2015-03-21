package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board

trait GameRepository {
  
  type GameId = Long
  
  def createGame(): GameId
  def getGameMetadata(gameId: GameId): GameMetadata
  def getMoves(gameId: GameId): Seq[Board]
  def saveMove(gameId: GameId, board: Board)
  def saveGame(gameId: GameId, boards: Seq[Board])
  def finishGame(gameId: GameId, game: GameMetadata)

}