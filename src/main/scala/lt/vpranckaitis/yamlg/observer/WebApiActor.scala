package lt.vpranckaitis.yamlg.observer

import lt.vpranckaitis.yamlg.observer.service.GameService
import spray.httpx.SprayJsonSupport.{sprayJsonMarshaller, sprayJsonUnmarshaller}
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import spray.routing.HttpServiceActor
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import lt.vpranckaitis.yamlg.game.Board
import scalaj.http.Http

class WebApiActor extends HttpServiceActor {
  
  val service = new GameService()
  
  def receive = runRoute {
    path("game" / "cpu" / IntNumber) { n =>
      get {
        val result = service.cpuVsCpuGame()
        complete(result._1.size + "\n" + (result._1 mkString "\n"))
      }
    } ~
    path("move" / Segment) { id =>
      post {
        entity(as[dto.Board]) { b =>
          val resp = Board(Http("http://localhost:5555/move/" + b.board).asString.body)
          complete(dto.BoardWithMoves(resp.board, resp.availableMoves))
          //complete(dto.Board("1111000011110000111100000000000000000000000022220000222200002222".reverse))
        }
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