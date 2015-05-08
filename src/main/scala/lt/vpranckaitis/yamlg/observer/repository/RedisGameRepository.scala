package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board
import org.sedis.Pool
import redis.clients.jedis.JedisPool
import spray.json._
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import scala.collection.JavaConverters._

class RedisGameRepository extends GameRepository {
  private[this] val redis = new Pool(new JedisPool("localhost", 6379))
  
  def startGame(started: Int): GameId = {
    redis withClient { j =>
      val gameId = j.incr(Keys.lastGameId)
      val metadata = Map(Keys.startedField -> started.toString,
                         Keys.winnerField -> "0");
      j.hmset(Keys.game(gameId), metadata)
      gameId
    }
  }
  def getGameMetadata(gameId: GameId): GameMetadata = {
    redis withClient { j =>
      val metadata = j.hgetAll(Keys.game(gameId))
      val turns = metadata.getOrElse(Keys.turnsField, j.llen(Keys.moves(gameId)).toString).toInt
      GameMetadata(gameId, metadata(Keys.startedField).toInt, metadata(Keys.winnerField).toInt, turns)
    }
  }
  
  def getMoves(gameId: GameId): Seq[Board] = {
    redis withClient { j =>
      j.lrange(Keys.moves(gameId), 0, -1) map { _.parseJson.convertTo[Board] }
    }
  }
  def saveMove(gameId: GameId, board: Board) {
    redis withClient { j =>
      j.rpush(Keys.moves(gameId), board.toJson.toString)
    }
  }
  def saveGame(gameId: GameId, started: Int, winner: Int, boards: Seq[Board]) {
    redis withClient { j =>
      val p = j.pipelined()
      p.rpush(Keys.moves(gameId), boards map { _.toJson.compactPrint }: _*)
      
      val metadata = Map(Keys.startedField -> started.toString,
                         Keys.winnerField -> winner.toString,
                         Keys.turnsField -> boards.size.toString);
      p.hmset(Keys.game(gameId), metadata.asJava)
      p.sync()
    }
  }
  def finishGame(gameId: GameId, winner: Int) {
    redis withClient { j =>
      val turns = j.llen(Keys.moves(gameId))
      val metadata = Map(Keys.turnsField -> turns.toString, Keys.winnerField -> winner.toString)
      j.hmset(Keys.game(gameId), metadata.asJava)
    }
  }
  
  object Keys {
    private val separator = ":"
    private val gameSegment = "game"
    private val lastIdSegment = "lastId"
    private val movesSegment = "moves"
    
    val startedField = "started"
    val winnerField = "winner"
    val turnsField = "turns"
    
    val lastGameId = gameSegment + separator + lastIdSegment
    def game(gameId: GameId) = gameSegment + separator + gameId.toString()
    def moves(gameId: GameId) = game(gameId) + separator + movesSegment
  }
}