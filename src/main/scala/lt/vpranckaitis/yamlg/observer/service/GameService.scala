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

class GameService(implicit system: ActorSystem) {
  
  import system.dispatcher
  
  implicit val timeout = Timeout(5000)
  
  val pipeline = sendReceive
  val movesPipeline = sendReceive ~> unmarshal[dto.Board]
  
  private[this] val repository: GameRepository = new RedisGameRepository
  
  def cpuVsCpuGame() = {
    
    @tailrec
    def gameTR(b: Board = Board.initial, player: Int = 1, moves: List[Board] = List()): (List[Board], Int) = {
      val future = if (player == 1) {
        movesPipeline(Get("http://localhost:5555/move/" + b.board)) map { x => Board(x.board) }
      } else {
        movesPipeline(Get("http://localhost:5555/move/" + b.reverse.board)) map { x => Board(x.board).reverse }
      }
      val b1 = Await.result(future, Duration("5s"))
      if (b1.isFinished != 0) {
        ((b1 :: moves).reverse, player)
      } else {
        gameTR(b1, player % 2 + 1, b1 :: moves)
      }
    }
    val gameId: GameRepository.GameId = repository.createGame(1)
    Future {
      val (boards, winner) = gameTR()
      repository.saveGame(gameId, 1, winner, boards map { b => dto.Board(b.board) })
      (boards, winner)
    }
    dto.GameMetadata(gameId, 1, 0)
  }
  
  def makeMove(b: dto.Board, gameId: GameRepository.GameId): Future[dto.BoardWithMoves] = {
    repository.saveMove(gameId, b)
    val b1 = movesPipeline(Get("http://localhost:5555/move/" + b.board))
    b1 map { x =>
      repository.saveMove(gameId, x)
      dto.BoardWithMoves(x.board, Board(x.board).availableMoves, Some(gameId))
    }
  }
  
  def createGame(game: dto.GameSetup): Future[dto.BoardWithMoves] = {
    val gameId = repository.createGame(if (game.playerFirst) 2 else 1)
    if (game.playerFirst) {
      Future(dto.BoardWithMoves(Board.initial.board, Board.initial.availableMoves, Some(gameId)))
    } else {
      makeMove(dto.Board(Board.initial.board), gameId) 
    }
  }
  
  def learnGame(gameId: Long): Future[String] = {
    val metadata = repository.getGameMetadata(gameId)
    val moves = repository.getMoves(gameId)
    val game = dto.Game(gameId, metadata.started, metadata.winner, moves.toList)
    pipeline(Put("http://localhost:5555/learn/game", game)).map { _.toString }
  }
}