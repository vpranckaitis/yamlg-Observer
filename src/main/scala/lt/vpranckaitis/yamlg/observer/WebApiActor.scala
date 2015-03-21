package lt.vpranckaitis.yamlg.observer

import spray.routing.HttpServiceActor
import scalaj.http.Http
import scala.annotation.tailrec

class WebApiActor extends HttpServiceActor {
  
  def cpuVsCpuGame(board: String) = {
    
    val switch = Map('1' -> '2', '2' -> '1', '0' -> '0')
    val player1Win = """^.*1{4}.{4}1{4}.{4}1{4}$"""
    val player2Win = """^2{4}.{4}2{4}.{4}2{4}.*$"""
    
    @tailrec
    def gameTR(b: String, n: Int): (String, Int, Int)  = {
      val b1 = Http("http://localhost:5555/move/" + b).asString.body
      val b1reversed = (b1 map switch).toList.reverse.mkString
      val b2reversed = Http("http://localhost:5555/move/" + b1reversed).asString.body
      val b2 = (b2reversed map switch).toList.reverse.mkString
      if (b1 matches player1Win ) {
        (b1, 1, n)
      } else if (b2 matches player2Win) {
        (b2, 2, n)
      } else {
        println(b2)
        gameTR(b2, n + 1)
      }
    }
    
    gameTR(board, 1)
  }
  
  def receive = runRoute {
    path("game" / "cpu" / IntNumber) { n =>
      get {
        val result = cpuVsCpuGame("1111000011110000111100000000000000000000000022220000222200002222")
        complete(result._1 + "  " + result._2 + "    " + result._3)
      }
    } ~
    path("move" / Segment) { b =>
      get {
        val resp = Http("http://localhost:5555/move/" + b).asString
        complete(resp.body)
      }
    } ~
    pathEndOrSingleSlash {
      (get | post) {
        getFromFile("ui/index.html")
      }
    } ~
    get { 
      getFromDirectory("ui")
    }
  }
}