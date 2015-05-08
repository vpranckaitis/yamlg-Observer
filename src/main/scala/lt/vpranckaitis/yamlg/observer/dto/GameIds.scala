package lt.vpranckaitis.yamlg.observer.dto

import lt.vpranckaitis.yamlg.observer.repository.GameRepository

case class GameIds(ids: Seq[GameRepository.GameId]) 