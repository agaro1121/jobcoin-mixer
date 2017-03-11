import models.jobcoin.{NewTransaction, NewTransactionResponse, SuccessResponse}
import org.scalatest.{Matchers, WordSpec}
import TestUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class JobcoinServiceSpec extends WordSpec with Matchers {

  "Jobcoin Service" should {
    "return a list of transactions" in withJobcoinService {
      service =>
        val transactions = Await.result(service.getTransactions, 3 seconds)

        transactions.isEmpty shouldBe false
        transactions(0).amount shouldBe "100"
        transactions(0).toAddress shouldBe "FirstAddress"
        transactions(0).timestamp shouldBe "2017-03-07T22:21:42.353Z"
        transactions(0).fromAddress shouldBe None
    }

    "return a success response when posting a transaction" in withJobcoinService {
      service =>
        val newTransaction = NewTransaction(
          fromAddress = "",
          toAddress = "",
          amount = "100"
        )

        val transactionResponse: NewTransactionResponse = Await.result(service.createNewTransaction(newTransaction), 3 seconds)
        transactionResponse.isInstanceOf[SuccessResponse] shouldBe true
    }
  }

}
