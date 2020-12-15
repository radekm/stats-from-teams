package cz.radekm

package object msTeams {
  type @@[A, B] = A { type Tag = B }

  implicit class TaggingExts[A](val x: A) extends AnyVal {
    def tagWith[B] = x.asInstanceOf[A @@ B]
  }

  type Token = String @@ "Token"
  type AppId = String @@ "AppId"
}
