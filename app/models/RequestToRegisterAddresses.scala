package models

import play.api.libs.json.Json

case class Address(address: String)
object Address {
  implicit lazy val addressJson = Json.format[Address]
}

case class RequestToRegisterAddresses(addresses: List[Address])
object RequestToRegisterAddresses {
  implicit lazy val requestJson = Json.format[RequestToRegisterAddresses]
}