package lt.vpranckaitis.yamlg.observer.dto

case class Bot(id: Option[Int], name: String, ip: String, port: Int, difficulty: Int, alive: Boolean)