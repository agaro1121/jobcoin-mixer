package actors

import java.time.Instant
import actors.AddressActor.{RegisteredDepositAddresses, DepositAndWithdrawalAddresses}
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{DiagnosticLoggingAdapter, Logging}
import models.Address
import models.jobcoin.{NewTransaction, Transaction}
import services.JobcoinService
import scala.util.{Failure, Success, Try}
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import cats._
import implicits._
import scala.concurrent.Future

object TransactionActor {
  case object PollForTransactions
  case object DoleOutToWithdrawalAccounts

  def props: Props = Props[TransactionActor]
}

class TransactionActor(jobcoinService: JobcoinService, addressActor: ActorRef) extends Actor {
  import TransactionActor._

  private val log: DiagnosticLoggingAdapter = Logging.getLogger(this)

  var allRelevantTransactions: List[Transaction] = List.empty[Transaction]
  var latestTimestamp: Instant = Instant.EPOCH
  var depositAddressAndBalance: Map[Address, BigDecimal] = Map.empty

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {

    case PollForTransactions =>
      val registeredDepositAddresses: Future[RegisteredDepositAddresses] = (addressActor ? AddressActor.GetRegisteredDepositAddresses).mapTo[RegisteredDepositAddresses]
      val allTransactionsFromJobcoin: Future[List[Transaction]] = jobcoinService.getTransactions

      registeredDepositAddresses.zip(allTransactionsFromJobcoin)
        .onComplete {
          case Success((depositAddresses, allTransactions)) => {
            val relevantTransactions =
              allTransactions
                .filter(transaction => Instant.parse(transaction.timestamp).isAfter(latestTimestamp))
                .filter(transaction => depositAddresses.addresses.contains(Address(transaction.toAddress)))

            allRelevantTransactions ++= relevantTransactions

            relevantTransactions.foreach { transaction =>
              jobcoinService.createNewTransaction(NewTransaction(transaction.toAddress, "TestHouseAccount", transaction.amount))
            }

            latestTimestamp = Try(allTransactions.map(transaction => Instant.parse(transaction.timestamp)).max).getOrElse(latestTimestamp)

            depositAddressAndBalance = Monoid[Map[Address, BigDecimal]].combine(
              depositAddressAndBalance,
              relevantTransactions.map(transaction => Address(transaction.toAddress) -> BigDecimal(transaction.amount)).toMap
            )

            log.info("All relevant transactions = {}", allRelevantTransactions)
            log.info("Latest Timestamp = {}", latestTimestamp)
            log.info("Registered Deposit Addresses And Balance = {}\n", depositAddressAndBalance)
          }

          case Failure(e) => log.error(e.getMessage); throw e
        }

    case DoleOutToWithdrawalAccounts =>
      val depositAndWithdrawalAddresses: Future[DepositAndWithdrawalAddresses] =
        (addressActor ? AddressActor.GetDepositAndWithdrawalAddresses).mapTo[DepositAndWithdrawalAddresses]

      depositAndWithdrawalAddresses.onComplete {
        case Success(data) =>
          val registeredDepositAndWithdrawAddresses: Map[Address, List[Address]] = data.addresses

          depositAddressAndBalance.foreach {
            case (depositAddress, balance) =>
              if (balance > 0) {

                val usersWithdrawalAddresses = registeredDepositAndWithdrawAddresses(depositAddress)
                val newBalance = if (balance >= 10) balance * 0.5 else BigDecimal(0)
                val amountToDoleOut = (balance - newBalance) / usersWithdrawalAddresses.size
                depositAddressAndBalance = depositAddressAndBalance.updated(depositAddress, newBalance)

                log.info("Deposit Address {} has new balance = {}", depositAddress,newBalance)
                log.info("Amount to be doled out to each address = {}\n", amountToDoleOut)

                usersWithdrawalAddresses.foreach { address =>
                  val newTransaction = NewTransaction("TestHouseAccount", address.address, amountToDoleOut.toString())
                  jobcoinService.createNewTransaction(newTransaction)
                }

              } else {
                log.info("DepositAddress({}) has been fulfilled. Removing from pending transactions...\n", depositAddress)
                depositAddressAndBalance = depositAddressAndBalance - depositAddress
              }

          }

        case Failure(e) => log.error(e.getMessage); throw e
      }

  }
}
