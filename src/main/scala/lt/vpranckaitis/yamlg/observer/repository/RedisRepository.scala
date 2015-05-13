package lt.vpranckaitis.yamlg.observer.repository

import lt.vpranckaitis.yamlg.observer.dto.GameMetadata
import lt.vpranckaitis.yamlg.observer.dto.Board
import org.sedis.Pool
import redis.clients.jedis.JedisPool
import spray.json._
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import scala.collection.JavaConverters._
import com.sun.media.sound.WaveExtensibleFileReader.GUID
import java.util.UUID
import scala.concurrent.Future
import scala.util.Success
import lt.vpranckaitis.yamlg.observer.dto.Bot
import com.typesafe.config.ConfigFactory
import java.io.File

class RedisRepository extends GameRepository with UserRepository {
  
  val (ip, port) = {
    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.load())
    val ip = config.getString("yamlg.observer.redis.ip")
    val port = config.getInt("yamlg.observer.redis.port")
    (ip, port)
  }
  
  private[this] val redis = new Pool(new JedisPool(ip, port))
  
  val sessionPeriod = 24 * 60 * 60; //seconds
  
  def startGame(started: Int, botId1: Int, botId2: Option[Int] = None): GameId = {
    val botGame = botId2 != None
    redis withClient { j =>
      val gameId = j.incr(Keys.lastGameId)
      val metadata = Map(Keys.startedField -> started.toString,
                         Keys.winnerField -> "0",
                         Keys.botsGameField -> botGame.toString,
                         Keys.botId1Field -> botId1.toString) ++ 
                         { botId2 map { Keys.botId2Field -> _.toString } };
      j.hmset(Keys.game(gameId), metadata)
      j.del(Keys.moves(gameId))
      gameId
    }
  }
  def getGameMetadata(gameId: GameId): GameMetadata = {
    redis withClient { j =>
      val metadata = j.hgetAll(Keys.game(gameId))
      val turns = metadata.getOrElse(Keys.turnsField, j.llen(Keys.moves(gameId)).toString).toInt
      val botsGame = metadata.get(Keys.botsGameField) map { _.toBoolean } getOrElse false
      val botId1 = metadata.get(Keys.botId1Field) map { _.toInt } getOrElse -1
      val botId2 = metadata.get(Keys.botId2Field) map { _.toInt }
      GameMetadata(gameId, metadata(Keys.startedField).toInt, metadata(Keys.winnerField).toInt, turns, botsGame, botId1, botId2)
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
  
  def createSession(email: String) = {
    redis withClient { j => 
      val sessionId = UUID.randomUUID().toString()
      j.setex(Keys.sessions(sessionId), sessionPeriod, email)
      sessionId
    }
  }
  
  def getUserEmail(sessionId: String): Option[String] = {
    redis withClient { j => 
      j.get(Keys.sessions(sessionId))
    }
  }
  
  def getLastGame(email: String): Option[GameId] = {
    println(email)
    redis withClient { j =>
      j.lindex(Keys.userGames(email), -1) match {
        case null => None
        case s => { println(s); Some(s.toLong) }
      }
    }
  }
  
  def getGames(email: String): Seq[GameId] = {
    println(email)
    redis withClient { j =>
      j.lrange(Keys.userGames(email), 0, -1) map { _.toLong }
    }
  }
  
  def assignGame(gameId: GameId, sessionId: String) {
    for (email <- getUserEmail(sessionId)) {
      redis withClient { j =>
        println("assignGame")
        j.rpush(Keys.userGames(email), gameId.toString)
      }
    }
  }
  
  def getLastGameId() = {
    redis withClient { j => 
      j.get(Keys.lastGameId) match {
        case Some(v) => v.toLong
        case _ => 0
      }
    }
  }
  
  def getBots() = {
    redis withClient { j => 
      val lastId = j.get(Keys.lastBotId) map { _.toInt } getOrElse 0
      (1 to lastId) map { id => getBot(id) } sortBy { _.id } toList
    }
  }
  
  def getBot(id: Int) = {
    redis withClient { j => 
      val fields = j.hgetAll(Keys.bots(id))
      Bot(Some(id), fields(Keys.name), fields(Keys.ip), fields(Keys.port).toInt, fields(Keys.difficulty).toInt, fields(Keys.alive).toBoolean)
    }
  }
  
  def createBot(name: String, ip: String, port: Int, difficulty: Int, alive: Boolean) = {
    redis withClient { j => 
      val id = j.incr(Keys.lastBotId).toInt
      val data = Map[String, String](Keys.name -> name,
                                     Keys.ip -> ip, 
                                     Keys.port -> port.toString,
                                     Keys.difficulty -> difficulty.toString,
                                     Keys.alive -> alive.toString) 
      j.hmset(Keys.bots(id), data.asJava)
      Bot(Some(id), name, ip, port, difficulty, alive)
    }
  }
  
  def updateBot(id: Int, name: String, ip: String, port: Int, difficulty: Int, alive: Boolean) = {
    redis withClient { j => 
      val data = Map[String, String](Keys.name -> name,
                                     Keys.ip -> ip, 
                                     Keys.port -> port.toString,
                                     Keys.difficulty -> difficulty.toString,
                                     Keys.alive -> alive.toString) 
      j.hmset(Keys.bots(id), data.asJava)
      Bot(Some(id), name, ip, port, difficulty, alive)
    }
  }
  
  def assignBotGame(gameId: GameId, botId: Int) {
    redis withClient { j => 
      j.rpush(Keys.botGames(botId), gameId.toString)
    }
  }
  
  object Keys {
    private val separator = ":"
    private val gameSegment = "game"
    private val lastIdSegment = "lastId"
    private val movesSegment = "moves"
    private val usersSegment = "users"
    private val sessionsSegment = "sessions"
    private val games = "games"
    private val botsSegment = "bots"
    
    val startedField = "started"
    val winnerField = "winner"
    val turnsField = "turns"
    val botsGameField = "botsGame"
    val botId1Field = "botId1"
    val botId2Field = "botId2"
    
    val name = "name"
    val ip = "ip"
    val port = "port"
    val difficulty = "difficulty"
    val alive = "alive"
    
    
    val lastGameId = gameSegment + separator + lastIdSegment
    def game(gameId: GameId) = gameSegment + separator + gameId.toString()
    def moves(gameId: GameId) = game(gameId) + separator + movesSegment
    def users(email: String) = usersSegment + separator + email
    def sessions(sessionId: String) = sessionsSegment + separator + sessionId
    def userGames(email: String) = users(email) + separator + games
    def bots() = botsSegment + separator + "*" 
    def bots(id: Int) = botsSegment + separator + id.toString 
    def botGames(id: Int) = bots(id) + separator + games
    val lastBotId = botsSegment + separator + lastIdSegment
  }
}