package lt.vpranckaitis.yamlg.observer

import akka.actor.ActorSystem
import akka.actor.Props
import akka.io.IO
import spray.can.Http
import scala.concurrent.duration.Duration

object Main {
  implicit val system = ActorSystem("yamlg-observer")
  
  def main(args: Array[String]) {
    val webApiActor = system.actorOf(Props(classOf[WebApiActor], system))
    
    IO(Http) ! Http.Bind(webApiActor, interface = "0.0.0.0", port = 8080)
    
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        IO(Http) ! Http.Unbind(Duration("3s"))
      }
    })
  }
}