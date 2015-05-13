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
import lt.vpranckaitis.yamlg.observer.dto.AccessToken
import com.typesafe.config.ConfigFactory
import java.io.File

class WebApiActor(implicit system: ActorSystem) extends HttpServiceActor {
  
  import system.dispatcher
  
  val service = new GameService()
  
  val (baseDirectory, userDirectory, userIndex) = {
    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.load())
    val base = config.getString("yamlg.observer.client.base-directory")
    val user = config.getString("yamlg.observer.client.user-directory")
    val index = config.getString("yamlg.observer.client.user-index")
    (base, user, index)
  }
  
  def receive = runRoute {
    pathPrefix("games") {
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
      path(LongNumber / "moves") { gameId =>
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
      path("difficulties") {
        get {
          onComplete(service.getDifficulties()) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(InternalServerError, ex)
          }
        }
      }
    } ~
    path("login"){
      post {
        entity(as[AccessToken]) { accessToken =>
          onComplete(service.login(accessToken.accessToken)) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(InternalServerError, ex)
          }
        }
      }
    } ~
    path("login" / Segment) { sessionId =>
      get {
        onComplete(service.relogin(sessionId)) {
          case Success(v) => complete(v)
          case Failure(ex) => complete(NotFound)
        }
      }
    } ~
    path("statistics" / Segment) { sessionId =>
      get {
        onComplete(service.getStatistics(sessionId)) {
          case Success(v) => complete(v)
          case Failure(ex) => complete(NotFound)
        }
      }
    } ~
    pathEndOrSingleSlash {
      (get | post) {
        getFromFile(userIndex)
      }
    } ~
    get { 
      getFromDirectory(userDirectory) ~ getFromDirectory(baseDirectory)
    }
  }
}