package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board
import org.sedis.Pool
import redis.clients.jedis.JedisPool

class RedisGameRepository extends GameRepository {
  private[this] val redis = new Pool(new JedisPool("localhost", 6379))
  
  def createGame(): GameId = {
    redis withClient { j =>
      j.incr(Keys.lastGameId)
    }
  }
  def getGameMetadata(gameId: GameId): GameMetadata = null
  def getMoves(gameId: GameId): Seq[Board] = null
  def saveMove(gameId: GameId, board: Board) {
    redis withClient { j =>
      j.lpush(Keys.moves(gameId), board.toString)
    }
  }
  def saveGame(gameId: GameId, boards: Seq[Board]) {
    redis withClient { j =>
      j.rpush(Keys.moves(gameId), boards map { _.toString }: _*)
    }
  }
  def finishGame(gameId: GameId, game: GameMetadata) {
    
  }
  
  object Keys {
    private val separator = ":"
    private val gameSegment = "game"
    private val lastIdSegment = "lastId"
    private val movesSegment = "moves"
    
    val lastGameId = gameSegment + separator + lastIdSegment
    def game(gameId: GameId) = gameSegment + separator + gameId.toString()
    def moves(gameId: GameId) = game(gameId) + separator + movesSegment
  }
}