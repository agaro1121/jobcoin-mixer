package controllers

import actors.{AddressActor, TransactionActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import com.google.inject.{Inject, Singleton}
import helpers.ControllerHelper
import models.{Address, Request}
import play.api.mvc.{Action, AnyContent, Controller}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import akka.util.Timeout
import models.jobcoin.NewTransaction
import services.JobcoinService
import scala.concurrent.duration._
import scala.language.postfixOps
import AddressActor.{AssignedDepositAddress, AssignAddresses}

//TODO: spread (20% - fee) between all of the users address
//TODO: find places of failure i.e user passes in empty list && take note
//TODO: address failures, maybe use cats validation?
//TODO: refactor - specifically renaming for now
//TODO: tests - validations
//TODO: jobcoin service unit tests
//TODO: jobcoin integration-tests (create transaction, get transaction)
//TODO: test routes?
//TODO: nice to have - swagger
//TODO: do I need an exception filter?
//TODO: make all logging akka logging

@Singleton
class JobcoinController @Inject() (actorSystem: ActorSystem, jobcoinService: JobcoinService) extends Controller with ControllerHelper {

  implicit val timeout: Timeout = 5 seconds

  val addressStore: ActorRef = actorSystem.actorOf(AddressActor.props)
  val transactionActor: ActorRef = actorSystem.actorOf(Props(new TransactionActor(jobcoinService, addressStore)))

  actorSystem.scheduler.schedule(5 seconds, 10 seconds, transactionActor, TransactionActor.PollForTransactions)
  actorSystem.scheduler.schedule(10 seconds, 20 seconds, transactionActor, TransactionActor.DoleOutToWithdrawalAccounts)

  def addAddress(): Action[AnyContent] = Action.async {
    implicit request =>
      val address: Address = getRequestItem[Address]

      Future(Ok(jsonify(address)))
  }

  def addAddresses(): Action[AnyContent] = Action.async {
    implicit request =>
      val req: Request = getRequestItem[Request]

      (addressStore ? AssignAddresses(req.addresses)).mapTo[AssignedDepositAddress]
        .map {
          assignedAddress =>
            Ok(jsonify(assignedAddress.address))
        }
  }

  def getTransactions() = Action.async {
    implicit request =>

      jobcoinService.getTransactions.map(r => Ok(jsonify(r)))
  }

  def postAmount() = Action.async {
    implicit request =>
      val req = getRequestItem[NewTransaction]

      jobcoinService.createNewTransaction(req).map(r => Ok(jsonify(r)))
  }

}
