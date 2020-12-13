package cz.radekm

import io.circe.generic.extras.Configuration

package object analyzer {
  implicit val circeGenericExtrasConfig: Configuration = Configuration.default
}
