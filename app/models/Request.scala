package models

import play.api.libs.json.Json

case class Address(address: String)
object Address {
  implicit lazy val addressJson = Json.format[Address]
}

case class Request(addresses: List[Address])
object Request {
  implicit lazy val requestJson = Json.format[Request]
}