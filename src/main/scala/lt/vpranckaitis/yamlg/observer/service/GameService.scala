package lt.vpranckaitis.yamlg.observer.service

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.util.Timeout
import lt.vpranckaitis.yamlg.game.Board
import lt.vpranckaitis.yamlg.observer.dto
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import lt.vpranckaitis.yamlg.observer.repository.{GameRepository, RedisRepository, UserRepository}
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._

object GameService {
  val PLAYER_WIN = 2;
  val PLAYER_LOSE = 1;
  val NO_WIN = 0;
}

class GameService(implicit system: ActorSystem) {
  
  import system.dispatcher
  import GameService._
  
  implicit val timeout = Timeout(5000)
  
  val GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo?access_token="
  
  val pipeline = sendReceive
  val movesPipeline = sendReceive ~> unmarshal[dto.Board]
  val loginPipeline = sendReceive ~> unmarshal[dto.GoogleAuth]
  
  private[this] val repository: RedisRepository = new RedisRepository
  
  def cpuVsCpuGame(botId1: Int, botId2: Int, n: Int) = {
    
    val url1 = getBotUrl(botId1) + "/move/"
    val url2 = getBotUrl(botId2) + "/move/"
    
    @tailrec
    def gameTR(b: Board = Board.initial, player: Int = Board.CPU, moves: List[Board] = List(), turnsLeft: Int = 1000): (List[Board], Int) = {
      if (turnsLeft == 0) {
        (moves.reverse, 0)
      } else {
        val future = if (player == 1) {
          movesPipeline(Get(url1 + b.board)) map { x => Board(x.board) }
        } else {
          movesPipeline(Get(url2 + b.reverse.board)) map { x => Board(x.board).reverse }
        }
        val b1 = Await.result(future, Duration("5s"))
        if (b1.isFinished != 0) {
          ((b1 :: moves).reverse, player)
        } else {
          gameTR(b1, player % 2 + 1, b1 :: moves, turnsLeft - 1)
        }
      }
    }
    
    Future {
      for (_ <- 1 to n) {
        val gameId = repository.startGame(1, botId1, Some(botId2))
        repository.assignBotGame(gameId, botId1)
        repository.assignBotGame(gameId, botId2)
        
        println(gameId)
        try {
      	  val (boards, winner) = gameTR()
          if (winner == 0) {
            println("aborted after 1000 moves")
            println(boards.takeRight(1).head.toString)
          }
      		repository.saveGame(gameId, 1, winner, boards map { b => dto.Board(b.board) })
      		(boards, winner)
        } catch {
          case e: Throwable => println(e)
        }
      }
    } 
    Future { "{\"aknowledged\": true}" }
  }
  
  def makeMove(b: dto.Board, gameId: GameRepository.GameId): Future[dto.BoardWithMoves] = {
    repository.saveMove(gameId, b)
    if (Board(b.board).isFinished != 0) {
      Future {
        repository.finishGame(gameId, PLAYER_WIN)
        dto.BoardWithMoves(b.board, Array.fill(65)(Array[Int]()), Some(gameId), PLAYER_WIN)
      }
    } else {
      val game = repository.getGameMetadata(gameId)
      val url = getBotUrl(game.botId1)
    	val b1 = movesPipeline(Get(url + "/move/" + b.board))
			b1 map { x =>
  			repository.saveMove(gameId, x)
        if (Board(x.board).isFinished == 0) {
  			  dto.BoardWithMoves(x.board, Board(x.board).availableMoves, Some(gameId))
        } else {
          repository.finishGame(gameId, PLAYER_LOSE)
          dto.BoardWithMoves(x.board, Array.fill(65)(Array[Int]()), Some(gameId), PLAYER_LOSE)
        }
    	}
    }
  }
  
  def createGame(game: dto.GameSetup): Future[dto.BoardWithMoves] = {
    val bot = repository.getBots() filter { _.difficulty == game.difficulty } head
    val gameId = repository.startGame(if (game.playerFirst) Board.PLAYER else Board.CPU, bot.id.get)
    
    for (botId <- bot.id) repository.assignBotGame(gameId, botId)
    for (sessionId <- game.sessionId) repository.assignGame(gameId, sessionId) 
    
    if (game.playerFirst) {
      Future(dto.BoardWithMoves(Board.initial.board, Board.initial.availableMoves, Some(gameId)))
    } else {
      makeMove(dto.Board(Board.initial.board), gameId) 
    }
  }
  
  def learnGame(gameId: Long): Future[String] = {
    val metadata = repository.getGameMetadata(gameId)
    val moves = repository.getMoves(gameId)
    if ((metadata.winner == 1 || metadata.winner == 2) && (moves.length < 300)) {
      val game = dto.Game(gameId, metadata.started, metadata.winner, moves.toList)
      pipeline(Put("http://localhost:5555/learn/game", game)).map { _.toString }
    } else {
      Future("No winner")
    }
  }
  
  def learnGames(botId: Int, gameIds: Seq[Long]): Future[String] = {
    val bot = repository.getBot(botId)
    val url = s"http://${bot.ip}:${bot.port}/learn/game"
    for (gameId <- gameIds) {
      val metadata = repository.getGameMetadata(gameId)
      val moves = repository.getMoves(gameId)
      if (metadata.winner == 1 || metadata.winner == 2) {
        val game = dto.Game(gameId, metadata.started, metadata.winner, moves.toList)
        pipeline(Put(url, game)).map { _.toString }
      }
    }
    Future("{\"aknowledged\":true}")
  }
  
  def login(accessToken: String): Future[dto.LoginInfo] = {
    for (googleAuth <- loginPipeline(Get(GOOGLE_USER_INFO_URL + accessToken))) yield {
      val session = repository.createSession(googleAuth.email)
      createLoginResponse(session, googleAuth.email)
    }
  }
  
  def relogin(sessionId: String): Future[dto.LoginInfo] = {
    Future { 
      repository.getUserEmail(sessionId) match {
        case None => throw new NullPointerException()
        case Some(email) => createLoginResponse(sessionId, email)
      }
    }
  }
  
  private def createLoginResponse(session: String, email: String) = {
    val boardWithMoves = repository.getLastGame(email) map { gameId => 
      println(gameId)
      val moves = repository.getMoves(gameId)
      val board: Board = if (moves.isEmpty) {
        Board.initial
      } else {
        Board(moves.last.board)
      }
      val availableMoves = if (board.isFinished == 0) {
        board.availableMoves
      } else {
        Array.fill(64)(Seq[Int]())
      }
      dto.BoardWithMoves(board.board, availableMoves, Some(gameId), board.isFinished)
    }
    
    dto.LoginInfo(session, email, boardWithMoves)
  }
  
  def getStatistics(sessionId: String) = {
    Future {
      repository.getUserEmail(sessionId) match {
        case None => { println("failed session"); dto.Statistics(0, 0, 0) }  
        case Some(email) => {
          repository.getGames(email) match {
            case list: Seq[Long] if (list.size == 0) => { println("no"); dto.Statistics(0, 0, 0) }
            case list: Seq[Long] => {
              val metadatas = list map { id => repository.getGameMetadata(id) }
              val finishedGames = metadatas filterNot { _.winner == 0 } map { _.turns }
              val average = if (finishedGames.size == 0) 
                0
              else 
                finishedGames.fold(0)(_ + _) / finishedGames.size
                
              val wins = metadatas filter { _.winner == 2 } size
              val loses = finishedGames.size - wins
              dto.Statistics(average, wins, loses) 
            }
          }
        }
      }
    }
  }
  
  def getGamesMetadata(offsetOption: Option[Long], limit: Long) = {
    Future {
      val lastGameId = repository.getLastGameId()
      
      val offset = offsetOption match {
        case Some(k) if (k < lastGameId) => k
        case _ => lastGameId
      }
      
      val gameIds = offset until Math.max(offset - limit, 0) by -1
      
      val metadatas = gameIds map { repository.getGameMetadata(_) }
      
      dto.GameMetadatas(metadatas, offset != lastGameId, offset - limit > 0)
    }
  }
  
  def getGame(gameId: Long) = {
    Future {
      val moves = repository.getMoves(gameId);
      val metadata = repository.getGameMetadata(gameId);
      dto.Game(gameId, metadata.started, metadata.winner, moves.toList)
    }
  }
  
  def getBots(): Future[Seq[dto.Bot]] = {
    Future {
      repository.getBots()
    }
  }
  
  def getBot(id: Int): Future[dto.Bot] = Future { repository.getBot(id) }
  
  def createBot(bot: dto.Bot): Future[dto.Bot] = {
    Future {
      repository.createBot(bot.name, bot.ip, bot.port, bot.difficulty, bot.alive)
    }
  }
  
  def updateBot(id: Int, bot: dto.Bot): Future[dto.Bot] = {
    Future {
      repository.getBot(id)
      repository.updateBot(id, bot.name, bot.ip, bot.port, bot.difficulty, bot.alive)
    }
  }
  
  def getDifficulties(): Future[List[Int]] = {
    Future {
      val r = { repository.getBots() map { _.difficulty } }
      r.distinct.sorted 
    }
  }
  
  private def getBotUrl(botId: Int) = {
    val bot = repository.getBot(botId)
    s"http://${bot.ip}:${bot.port}"
  }
}