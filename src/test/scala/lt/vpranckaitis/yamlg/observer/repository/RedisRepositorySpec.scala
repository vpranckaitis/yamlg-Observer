package lt.vpranckaitis.yamlg.observer.repository

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import lt.vpranckaitis.scalatest.tags.IntegrationTest

class RedisRepositorySpec extends FlatSpec with Matchers {
  "RedisRepository" should "start game correctly" taggedAs(IntegrationTest) in {
    val redis = new RedisRepository()
    val started = 1
    val botId1 = 5
    val botId2 = 7
    val id = redis.startGame(started, botId1, Some(botId2))
    val metadata = redis.getGameMetadata(id)
    metadata.started should be (started)
    metadata.botId1 should be (botId1)
    metadata.botId2 should be (Some(botId2))
  }
}