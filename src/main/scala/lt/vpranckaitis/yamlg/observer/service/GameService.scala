package lt.vpranckaitis.yamlg.observer.service

import scala.annotation.tailrec
import lt.vpranckaitis.yamlg.game.Board
import lt.vpranckaitis.yamlg.observer.repository.{GameRepository, RedisGameRepository}
import lt.vpranckaitis.yamlg.observer.dto
import scalaj.http.Http

class GameService {
  
  private[this] val repository: GameRepository = new RedisGameRepository
  
  def cpuVsCpuGame() = {
    
    @tailrec
    def gameTR(b: Board = Board.initial, player: Int = 1, moves: List[Board] = List()): (List[Board], Int) = {
      val b1 = if (player == 1) {
        Board(Http("http://localhost:5555/move/" + b.board).asString.body)
      } else {
        Board(Http("http://localhost:5555/move/" + b.reverse.board).asString.body).reverse
      }
      if (b1.isFinished != 0) {
        ((b1 :: moves).reverse, player)
      } else {
        gameTR(b1, player % 2 + 1, b1 :: moves)
      }
    }
    val (boards, winner) = gameTR()
    val gameId = repository.createGame()
    repository.saveGame(gameId, boards map { b => dto.Board(b.board) })
    (boards, winner)
  }
  
  def makeMove(b: dto.Board): dto.BoardWithMoves = {
    val b1 = Board(Http("http://localhost:5555/move/" + b.board).asString.body)
    dto.BoardWithMoves(b1.board, b1.availableMoves)
  }
}