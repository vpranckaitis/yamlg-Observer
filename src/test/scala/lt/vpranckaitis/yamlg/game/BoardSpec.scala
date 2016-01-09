package lt.vpranckaitis.yamlg.game

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class BoardSpec extends FlatSpec with Matchers {
  "Board" should "recognize when '1' wins" in {
    val board = "2222000022220000220220000000000000000000000011110000111100001111";
    
    Board(board).isFinished should be (1)
  }
  
  it should "recognize when '2' wins" in {
    val board = "2222000022220000222200000000000000000000000110110000111100001111";
    
    Board(board).isFinished should be (2)
  }
  
  it should "recognize that nobody wins" in {
    val board = "2222000022200000222200000200000000000000000110110000111100001111";
    
    Board(board).isFinished should be (0)
  }
}