package controllers

import actors.{AddressActor, TransactionActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import com.google.inject.{Inject, Singleton}
import helpers.ControllerHelper
import models.RequestToRegisterAddresses
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import akka.util.Timeout
import services.JobcoinService
import scala.concurrent.duration._
import scala.language.postfixOps
import AddressActor.{RegisterUserWithdrawalAddresses, RegisteredDepositAddress}

//TODO: spread (20% - fee) between all of the users address
//TODO: find places of failure take note
//TODO: address failures, maybe use cats validation?
//TODO: refactor - specifically renaming for now
//TODO: tests - validations
//TODO: jobcoin service unit tests
//TODO: jobcoin integration-tests (create transaction, get transaction)
//TODO: test routes?
//TODO: nice to have - swagger
//TODO: make all logging akka logging

@Singleton
class JobcoinController @Inject() (actorSystem: ActorSystem, jobcoinService: JobcoinService) extends Controller with ControllerHelper {

  implicit val timeout: Timeout = 5 seconds

  val addressStore: ActorRef = actorSystem.actorOf(AddressActor.props)
  val transactionManager: ActorRef = actorSystem.actorOf(Props(new TransactionActor(jobcoinService, addressStore)))

  actorSystem.scheduler.schedule(5 seconds, 10 seconds, transactionManager, TransactionActor.PollForTransactions)
  actorSystem.scheduler.schedule(10 seconds, 20 seconds, transactionManager, TransactionActor.DoleOutToWithdrawalAccounts)

  def addAddresses() = Action.async {
    implicit request =>
      val req: RequestToRegisterAddresses = getRequestItem[RequestToRegisterAddresses]

      req.addresses match {
        case Nil => Future.successful(BadRequest("Your request contains an empty list of withdrawal addresses"))
        case addresses @ _ :: _ =>
          (addressStore ? RegisterUserWithdrawalAddresses(addresses)).mapTo[RegisteredDepositAddress]
            .map {
              depositAddress =>
                Ok(jsonify(depositAddress.address))
            }
      }

  }

}
