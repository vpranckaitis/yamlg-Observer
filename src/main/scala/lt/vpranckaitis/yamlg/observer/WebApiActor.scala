package lt.vpranckaitis.yamlg.observer

import spray.routing.HttpServiceActor
import scalaj.http.Http

class WebApiActor extends HttpServiceActor {
  def receive = runRoute {
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