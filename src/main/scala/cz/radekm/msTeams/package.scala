package cz.radekm

import io.circe.{Decoder, Encoder}

package object msTeams {
  type @@[A, B] = A { type Tag = B }

  implicit class TaggingExts[A](val x: A) extends AnyVal {
    def tagWith[B] = x.asInstanceOf[A @@ B]
  }

  type Token = String @@ "Token"
  type AppId = String @@ "AppId"

  // Json serialization.
  implicit def encodeTaggedString[B]: Encoder[String @@ B] = Encoder.encodeString.contramap[String @@ B] { x => x }
  implicit def decodeTaggedString[B]: Decoder[String @@ B] = Decoder.decodeString.map[String @@ B] { x => x.tagWith[B] }
}
