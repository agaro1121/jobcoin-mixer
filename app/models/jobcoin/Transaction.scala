package models.jobcoin

import play.api.libs.json.{Format, Json}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Transaction(
  timestamp: String,
  fromAddress: Option[String], //Some transactions lack a “fromAddress” - those are ones where Jobcoins were created from the UI.
  toAddress: String,
  amount: String
)

object Transaction {
  implicit lazy val TransactionJson: Format[Transaction] = Json.format[Transaction]
}

case class NewTransaction(
  fromAddress: String,
  toAddress: String,
  amount: String
)

object NewTransaction {
  implicit lazy val TransactionJson: Format[NewTransaction] = Json.format[NewTransaction]
}

sealed trait TransactionPostResponse
case class SuccessResponse(status: String) extends TransactionPostResponse
object SuccessResponse {
  implicit lazy val successResponseJson: Format[SuccessResponse] = Json.format[SuccessResponse]
}

case class ErrorResponse(error: String) extends TransactionPostResponse
object ErrorResponse {
  implicit lazy val errorResponseJson: Format[ErrorResponse] = Json.format[ErrorResponse]
}

object TransactionPostResponse {
  implicit lazy val reads: Reads[TransactionPostResponse] = {
    val success = Json.reads[SuccessResponse]
    val failure = Json.reads[ErrorResponse]
    __.read[SuccessResponse](success).map(x => x: TransactionPostResponse) |
      __.read[ErrorResponse](failure).map(x => x: TransactionPostResponse)
  }

  implicit lazy val writes: Writes[TransactionPostResponse] = Writes[TransactionPostResponse] {
    case success: SuccessResponse => Json.writes[SuccessResponse].writes(success)
    case failure: ErrorResponse => Json.writes[ErrorResponse].writes(failure)
  }
}
