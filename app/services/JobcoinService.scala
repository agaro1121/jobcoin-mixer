package services

import com.google.inject.Inject
import com.google.inject.name.Named
import helpers.JobcoinConfig
import models.jobcoin._
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global

class JobcoinService @Inject() (
  @Named("jobcoinConfig") config: JobcoinConfig,
  ws: WSClient
) {

  def getTransactions: Future[List[Transaction]] = {
    val fullUrl = s"${config.url}${config.transactionEndpoint}"
    def getTransactionsWithRetries(attemptsLeft: Int): Future[WSResponse] = {
      ws.url(fullUrl)
        .get()
        .recoverWith {
          case _: TimeoutException if attemptsLeft > 0 =>
            Logger.warn(s"Timeout while calling $fullUrl. $attemptsLeft left. Retrying...")
            getTransactionsWithRetries(attemptsLeft - 1)

          case throwable => throw throwable
        }
    }

    getTransactionsWithRetries(config.retries).map {
      response =>
        response.status match {
          case Status.OK => response.json.as[List[Transaction]]
          case _ => throw new Exception(s"Something went wrong. Response=${response.body}")
        }
    }
  }

  def createNewTransaction(newTransaction: NewTransaction): Future[TransactionPostResponse] = {
    val fullUrl = s"${config.url}${config.transactionEndpoint}"

    def createNewTransactionWithRetries(attemptsLeft: Int): Future[WSResponse] = {
      ws.url(fullUrl)
        .post(Json.toJson(newTransaction))
        .recoverWith {
          case _: TimeoutException if attemptsLeft > 0 =>
            Logger.warn(s"Timeout while calling $fullUrl. $attemptsLeft left. Retrying...")
            createNewTransactionWithRetries(attemptsLeft - 1)

          case throwable => throw throwable
        }
    }

    createNewTransactionWithRetries(config.retries).map {
      response =>
        //These cases look a little crazy but we need to help out the compiler a bit
        response.status match {
          case Status.OK =>
            val successResponse: TransactionPostResponse = response.json.as[SuccessResponse]
            successResponse

          case Status.PAYMENT_REQUIRED =>
            val errorResponse: TransactionPostResponse = response.json.as[ErrorResponse]
            errorResponse

          case _ => throw new Exception(s"Something went wrong with the call to $fullUrl")
        }
    }
  }

}
