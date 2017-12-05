package io.sudostream.classtimetable.config

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class ActorSystemWrapper(configHelper: ConfigHelper) {
  lazy val system = ActorSystem("classtimetable-writer-system", configHelper.config)
  implicit val actorSystem = system
  lazy val materializer = ActorMaterializer()
}
