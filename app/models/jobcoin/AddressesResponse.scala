package models.jobcoin

import play.api.libs.json.{Format, Json}

case class AddressesResponse(
  balance: String,
  transactions: List[Transaction]
)

object AddressesResponse {
  implicit lazy val AddressesResponseJson: Format[AddressesResponse] = Json.format[AddressesResponse]
}