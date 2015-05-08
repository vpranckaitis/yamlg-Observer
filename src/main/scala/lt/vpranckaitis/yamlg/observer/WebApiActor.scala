package lt.vpranckaitis.yamlg.observer

import lt.vpranckaitis.yamlg.observer.service.GameService
import spray.httpx.SprayJsonSupport.{sprayJsonMarshaller, sprayJsonUnmarshaller}
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import spray.routing.HttpServiceActor
import lt.vpranckaitis.yamlg.observer.dto.ExtendedJsonProtocol._
import lt.vpranckaitis.yamlg.game.Board
import scalaj.http.Http
import akka.actor.ActorSystem
import scala.util.Success
import scala.util.Failure
import scala.util.Success
import spray.http.StatusCodes._

class WebApiActor(implicit system: ActorSystem) extends HttpServiceActor {
  
  import system.dispatcher
  
  val service = new GameService()
  
  def receive = runRoute {
    pathPrefix("game") {
      pathEndOrSingleSlash {
        post {
          entity(as[dto.GameSetup]) { game =>
            onComplete(service.createGame(game)) {
              case Success(b) => complete(Created, b)
              case _ => complete(InternalServerError, "failed")
            }
          }
        }
      } ~
      path(LongNumber / "move") { gameId =>
        post {
          entity(as[dto.Board]) { b =>
            onComplete(service.makeMove(b, gameId)) {
              case Success(b) => complete(b)
              case _ => complete(InternalServerError, "failed")
            }
          }
        }
      } ~
      path(LongNumber / "learn") { gameId =>
        put {
          onComplete(service.learnGame(gameId)) {
            case Success(b) => complete(b)
            case _ => complete(InternalServerError, "failed")
          }
        }
      } ~
      path("learn") {
        put {
          onComplete(service.learnGames()) {
            case Success(b) => complete(b)
            case _ => complete(InternalServerError, "failed")
          }
        }
      } 
    } ~
    path("cpugame" / IntNumber) { n =>
      get {
        onComplete(service.cpuVsCpuGame(n)) {
          case Success(b) => complete(b)
          case _ => complete(InternalServerError, "failed")
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