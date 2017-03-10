package actors

import java.time.Instant
import actors.AddressActor.{AssignedDepositAddresses, DepositAndWithdrawalAddresses}
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

  private var allRelevantTransactions: List[Transaction] = List.empty[Transaction]
  private var latestTimestamp: Instant = Instant.EPOCH
  private var depositAddressAndBalance: Map[Address, BigDecimal] = Map.empty

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {

    case PollForTransactions =>
      val depositAddressesPairedToUsers: Future[AssignedDepositAddresses] = (addressActor ? AddressActor.GetDepositAddresses).mapTo[AssignedDepositAddresses]
      val allTransactionsFromJobcoin: Future[List[Transaction]] = jobcoinService.getTransactions

      depositAddressesPairedToUsers.zip(allTransactionsFromJobcoin)
        .onComplete {
          case Success((depositAddresses, allTransactions)) => {
            log.info("depositAddresses = " + depositAddresses)
            val relevantTransactions =
              allTransactions
                .filter(transaction => Instant.parse(transaction.timestamp).isAfter(latestTimestamp))
                .filter(transaction => depositAddresses.addresses.contains(Address(transaction.toAddress)))

            allRelevantTransactions ++= relevantTransactions
            log.info("All relevant transactions = {}", allRelevantTransactions)

            relevantTransactions.foreach { transaction =>
              jobcoinService.createNewTransaction(NewTransaction(transaction.toAddress, "TestHouseAccount", transaction.amount))
            }

            latestTimestamp = Try(allTransactions.map(transaction => Instant.parse(transaction.timestamp)).max).getOrElse(latestTimestamp)
            log.info("latestTimestamp = {}", latestTimestamp)

            depositAddressAndBalance = Monoid[Map[Address, BigDecimal]].combine(
              depositAddressAndBalance,
              relevantTransactions.map(transaction => Address(transaction.toAddress) -> BigDecimal(transaction.amount)).toMap
            )
            log.info("depositAddressAndBalance = {}", depositAddressAndBalance)
          }

          case Failure(e) => log.error(e.getMessage); throw e
        }

    case DoleOutToWithdrawalAccounts =>
      val depositAndWithdrawalAddresses: Future[DepositAndWithdrawalAddresses] =
        (addressActor ? AddressActor.GetDepositAndWithdrawalAddresses).mapTo[DepositAndWithdrawalAddresses]

      depositAndWithdrawalAddresses.onComplete {
        case Success(data) =>
          val depositAddressAndUsersWithdrawalAddresses: Map[Address, List[Address]] = data.addresses
          log.info("depositAddress and withdrawal addresses = {}", depositAddressAndUsersWithdrawalAddresses.mkString("\n"))

          depositAddressAndBalance.foreach {
            case (depositAddress, balance) =>
              if (balance > 0) {

                val usersWithdrawalAddresses = depositAddressAndUsersWithdrawalAddresses(depositAddress)
                val newBalance = if (balance >= 10) balance * 0.5 else BigDecimal(0)
                val amountToDoleOut = (balance - newBalance) / usersWithdrawalAddresses.size
                depositAddressAndBalance = depositAddressAndBalance.updated(depositAddress, newBalance)

                log.info("new balance = {}", newBalance)
                log.info("amount to be doled out = {}", amountToDoleOut)
                log.info("updated depositAddressAndBalance = {}", depositAddressAndBalance.mkString)

                usersWithdrawalAddresses.foreach { address =>
                  val newTransaction = NewTransaction("TestHouseAccount", address.address, amountToDoleOut.toString())
                  log.info("new transaction = {}", newTransaction)
                  jobcoinService.createNewTransaction(newTransaction) //TODO: onFailure return funds
                }

              } else {
                log.info("DepositAddress({}) has been fulfilled. Removing from pending transactions...", depositAddress)
                depositAddressAndBalance = depositAddressAndBalance - depositAddress
              }

          }

        case Failure(e) => log.error(e.getMessage); throw e
      }

  }
}
