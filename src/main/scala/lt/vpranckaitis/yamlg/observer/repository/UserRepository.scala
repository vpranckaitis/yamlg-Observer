package lt.vpranckaitis.yamlg.observer.repository

import scala.concurrent.Future

trait UserRepository {
  def createSession(email: String): String
}