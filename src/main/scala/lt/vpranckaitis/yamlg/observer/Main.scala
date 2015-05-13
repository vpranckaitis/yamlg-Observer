package lt.vpranckaitis.yamlg.observer

import akka.actor.ActorSystem
import akka.actor.Props
import akka.io.IO
import spray.can.Http
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import java.io.File

object Main {
  implicit val system = ActorSystem("yamlg-observer")
  
  val (ip, port, adminIp, adminPort) = {
    val config = ConfigFactory.parseFile(new File("application.conf")).
                               withFallback(ConfigFactory.load()).
                               getConfig("yamlg.observer.server")
    val ip = config.getString("ip")
    val port = config.getInt("port")
    val adminIp = config.getString("admin-ip")
    val adminPort = config.getInt("admin-port")
    (ip, port, adminIp, adminPort)
  }
  
  def main(args: Array[String]) {
    val webApiActor = system.actorOf(Props(classOf[WebApiActor], system))
    val adminApiActor = system.actorOf(Props(classOf[AdminApiActor], system))
    
    IO(Http) ! Http.Bind(webApiActor, interface = ip, port = port)
    IO(Http) ! Http.Bind(adminApiActor, interface = adminIp, port = adminPort)
    
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        IO(Http) ! Http.Unbind(Duration("3s"))
      }
    })
  }
}