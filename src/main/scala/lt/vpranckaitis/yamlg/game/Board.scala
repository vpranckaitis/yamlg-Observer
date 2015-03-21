package lt.vpranckaitis.yamlg.game

import scala.annotation.tailrec

object Board {
  val initial = Board("1111000011110000111100000000000000000000000022220000222200002222")
  val width = 8
  val height = 8
  val size = width * height
  
  val moves = List((0, -1), (0, 1), (-1, 0), (1, 0))
  
  def posToCoord(p: Int) = (p % width, p / width)
  def coordToPos(x: Int, y: Int) = y * width + x
  
  def isInside(x: Int, y: Int) =  x >= 0 && x < width && y >= 0 && y < height
}

case class Board(board: String) {
  def reverse(): Board = {
    val recolorCheckers = Map('1' -> '2', '2' -> '1', '0' -> '0')
    Board(board.reverse map recolorCheckers)
  }
  
  lazy val isFinished = {
    val player1Win = """^.*1{4}.{4}1{4}.{4}1{4}$"""
    val player2Win = """^2{4}.{4}2{4}.{4}2{4}.*$"""
    if (board matches player1Win) { 
      1 
    } else if (board matches player2Win) { 
      2
    } else { 
      0
    }
  }
  
  lazy val availableMoves = {
    val moves = for {
      p <- 0 until Board.size
    } yield availableMovesFrom(p)
    moves.toArray
  }
  
  private def availableMovesFrom(p: Int, player: Char = '2') = {
    if (board.charAt(p) == player) {
      step(p) ++ jump(p)
    } else {
      Seq[Int]()
    }
  }
  
  private def step(p: Int) = {
    val (x, y) = Board.posToCoord(p)
    for {
      (dx, dy) <- Board.moves
      p1 = Board.coordToPos(x + dx, y + dy)
      if Board.isInside(x + dx, y + dy) && board.charAt(p1) == '0'
    } yield p1
  }
  
  private def jump(p: Int) = {
    @tailrec
    def jumpTR(q: List[Int] = List(p), visited: Set[Int] = Set(p)): Seq[Int] = {
      if (q.isEmpty) {
        (visited - p).toSeq
      } else {
        val pt = q.head
        val (x, y) = Board.posToCoord(pt)
        val next = for {
          (dx, dy) <- Board.moves
          p1 = Board.coordToPos(x + dx, y + dy)
          p2 = Board.coordToPos(x + 2*dx, y + 2*dy)
          if Board.isInside(x + 2 * dx, y + 2 * dy) && 
             board.charAt(p1) != '0' && 
             board.charAt(p2) == '0' &&
             !visited.contains(p2)
        } yield p2
        
        jumpTR(next ::: q.tail, visited ++ next)
      }
    }
    
    jumpTR()
  }
}