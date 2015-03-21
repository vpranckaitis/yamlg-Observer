package lt.vpranckaitis.yamlg.observer.dto

case class Board(board: String) {
  def reverse(): Board = {
    val recolorCheckers = Map('1' -> '2', '2' -> '1', '0' -> '0')
    Board(board.reverse map recolorCheckers)
  }
}