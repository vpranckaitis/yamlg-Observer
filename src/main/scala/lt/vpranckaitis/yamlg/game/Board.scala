package lt.vpranckaitis.yamlg.game

object Board {
  val initial = Board("1111000011110000111100000000000000000000000022220000222200002222")
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
}