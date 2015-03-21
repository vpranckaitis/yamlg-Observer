package lt.vpranckaitis.yamlg.observer

import spray.routing.HttpServiceActor
import scalaj.http.Http
import scala.annotation.tailrec
import lt.vpranckaitis.yamlg.observer.service.GameService
import lt.vpranckaitis.yamlg.observer.service.GameService

class WebApiActor extends HttpServiceActor {
  
  val service = new GameService()
  
  def receive = runRoute {
    path("game" / "cpu" / IntNumber) { n =>
      get {
        val result = service.cpuVsCpuGame()
        complete(result._1.size + "\n" + (result._1 mkString "\n"))
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