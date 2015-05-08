package lt.vpranckaitis.yamlg.observer.service

import scala.annotation.tailrec
import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import lt.vpranckaitis.yamlg.game.Board
import lt.vpranckaitis.yamlg.observer.dto
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import lt.vpranckaitis.yamlg.observer.repository.{GameRepository, RedisGameRepository}
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.can.Http
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future

object GameService {
  val PLAYER_WIN = -1;
  val PLAYER_LOSE = 1;
  val NO_WIN = 0;
}

class GameService(implicit system: ActorSystem) {
  
  import system.dispatcher
  import GameService._
  
  implicit val timeout = Timeout(5000)
  
  val pipeline = sendReceive
  val movesPipeline = sendReceive ~> unmarshal[dto.Board]
  
  private[this] val repository: GameRepository = new RedisGameRepository
  
  def cpuVsCpuGame(n: Int) = {
    
    @tailrec
    def gameTR(b: Board = Board.initial, player: Int = Board.CPU, moves: List[Board] = List(), turnsLeft: Int = 1000): (List[Board], Int) = {
      if (turnsLeft == 0) {
        (moves.reverse, 0)
      } else {
        val future = if (player == 1) {
          movesPipeline(Get("http://localhost:5555/move/" + b.board)) map { x => Board(x.board) }
        } else {
          movesPipeline(Get("http://localhost:5555/move/" + b.reverse.board)) map { x => Board(x.board).reverse }
        }
        val b1 = Await.result(future, Duration("5s"))
        if (b1.isFinished != 0) {
          ((b1 :: moves).reverse, player)
        } else {
          gameTR(b1, player % 2 + 1, b1 :: moves, turnsLeft - 1)
        }
      }
    }
    
    val ids = for (_ <- 1 to n) yield repository.startGame(1)
    Future {
      for (gameId <- ids) {
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
    Future { dto.GameIds(ids) }
  }
  
  def makeMove(b: dto.Board, gameId: GameRepository.GameId): Future[dto.BoardWithMoves] = {
    repository.saveMove(gameId, b)
    if (Board(b.board).isFinished != 0) {
      Future {
        dto.BoardWithMoves(b.board, Array(), Some(gameId), PLAYER_WIN)
      }
    } else {
    	val b1 = movesPipeline(Get("http://localhost:5555/move/" + b.board))
			b1 map { x =>
  			repository.saveMove(gameId, x)
        if (Board(b.board).isFinished == 0) {
  			  dto.BoardWithMoves(x.board, Board(x.board).availableMoves, Some(gameId))
        } else {
          dto.BoardWithMoves(x.board, Array(), Some(gameId), PLAYER_LOSE)
        }
    	}
    }
  }
  
  def createGame(game: dto.GameSetup): Future[dto.BoardWithMoves] = {
    val gameId = repository.startGame(if (game.playerFirst) Board.PLAYER else Board.CPU)
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
  
  def learnGames(): Future[String] = {
    for (i <- 500 to 2000) {
      learnGame(i)
    }
    Future("Done")
  }
}