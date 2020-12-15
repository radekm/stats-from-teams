package cz.radekm.analyzer

import cz.radekm.msTeams.{@@, TaggingExts}
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.syntax._
import monix.eval.Task

import java.nio.file.{Files, Path}

object Json {
  implicit val circeGenericExtrasConfig: Configuration = Configuration.default

  implicit def encodeTaggedString[B]: Encoder[String @@ B] = Encoder.encodeString.contramap[String @@ B] { x => x }
  implicit def decodeTaggedString[B]: Decoder[String @@ B] = Decoder.decodeString.map[String @@ B] { x => x.tagWith[B] }

  def saveToFile(file: Path, conversations: AllConversations): Task[Unit] = Task {
    val json = conversations.asJson.spaces4
    Files.writeString(file, json)
  }

  def loadFromFile(file: Path): Task[AllConversations] = Task {
    val json = Files.readString(file)
    decode[AllConversations](json)
  }.flatMap { Task.fromEither(_) }
}
