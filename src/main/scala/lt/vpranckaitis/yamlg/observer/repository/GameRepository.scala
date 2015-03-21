package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board

trait GameRepository {
  
  def createGame(): Int
  def getGameMetadata(gameId: Int): GameMetadata
  def getGameMoves(gameId: Int): Seq[Board]
  def saveMove(gameId: Int)
  def finishGame(gameId: Int)

}