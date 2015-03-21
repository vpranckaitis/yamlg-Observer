package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board

class RedisGameRepository extends GameRepository {
  def createGame(): Int = 0
  def getGameMetadata(gameId: Int): GameMetadata = null
  def getGameMoves(gameId: Int): Seq[Board] = null
  def saveMove(gameId: Int) {}
  def finishGame(gameId: Int) {}
}