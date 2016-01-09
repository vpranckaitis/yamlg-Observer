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
import spray.http.StatusCodes
import lt.vpranckaitis.yamlg.observer.dto._
import com.typesafe.config.ConfigFactory
import java.io.File

class AdminApiActor(implicit system: ActorSystem) extends HttpServiceActor {
  
  import system.dispatcher
  
  val (baseDirectory, adminDirectory, adminIndex) = {
    val config = ConfigFactory.parseFile(new File("application.conf")).withFallback(ConfigFactory.load())
    val base = config.getString("yamlg.observer.client.base-directory")
    val admin = config.getString("yamlg.observer.client.admin-directory")
    val index = config.getString("yamlg.observer.client.admin-index")
    (base, admin, index)
  }
  
  val service = new GameService()
  
  def receive = runRoute {
    path("games") {
      get {
        parameters("offset".as[Long].?, "limit".as[Long] ? 10) { (offsetOption, limit) => 
          onComplete(service.getGamesMetadata(offsetOption, limit)) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(InternalServerError, ex)
          }
        }
      }
    } ~
    path("games" / LongNumber) { gameId =>
      get {
        onComplete(service.getGame(gameId)) {
          case Success(v) => complete(v)
          case Failure(ex) => complete(InternalServerError, ex)
        }
      }
    } ~
    path("bots") {
      get {
        onComplete(service.getBots()) {
          case Success(v) => complete(v)
          case Failure(ex) => complete(InternalServerError, ex)
        }
      } ~
      post {
        entity(as[Bot]) { bot =>
          onComplete(service.createBot(bot)) {
            case Success(v) => complete(Created, v)
            case Failure(ex) => complete(InternalServerError, ex)
          }
        }
      }
    } ~
    path("bots" / IntNumber) { botId =>
      put {
        entity(as[Bot]) { bot =>
          onComplete(service.updateBot(botId, bot)) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(NotFound, "Bot not found")
          }
        }
      }
    } ~
    path("bots" / IntNumber / "learn") { botId =>
      put {
        entity(as[GameIds]) { gameIds =>
          onComplete(service.learnGames(botId, gameIds.ids)) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(NotFound, "Bot not found")
          }
        }
      }
    } ~
    path("bots" / IntNumber / "learnall") { botId =>
      put {
        onComplete(service.learnGames(botId)) {
          case Success(v) => complete(v)
          case Failure(ex) => complete(NotFound, "Bot not found")
        }
      }
    } ~
    path("botgame") {
      post {
        entity(as[BotGameSetup]) { gameSetup =>
          onComplete(service.cpuVsCpuGame(gameSetup.botId1, gameSetup.botId2, gameSetup.count)) {
            case Success(v) => complete(v)
            case Failure(ex) => complete(NotFound, "Bot not found")
          }
        }
      }
    } ~
    pathEndOrSingleSlash {
      (get | post) {
        getFromFile(adminIndex)
      }
    } ~
    get { 
      getFromDirectory(adminDirectory) ~ getFromDirectory(baseDirectory)
    }
  }
}