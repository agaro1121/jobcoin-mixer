package actors

import akka.actor.{Actor, Props}
import models.Address

import scala.collection.immutable.Seq

object AddressActor {
  case class AssignAddresses(addresses: List[Address])
  case object GetDepositAddresses
  case object GetDepositAndWithdrawalAddresses
  case class AssignedDepositAddresses(addresses: List[Address])
  case class AssignedDepositAddress(address: Address)
  case class DepositAndWithdrawalAddresses(addresses: Map[Address, List[Address]])

  def props: Props = Props[AddressActor]
}

class AddressActor extends Actor {

  import AddressActor._

  var unusedAddresses: Seq[Address] = List.fill(1000)(Address(java.util.UUID.randomUUID.toString))
  var pairedAddresses: Map[Address, List[Address]] = Map.empty

  override def receive: Receive = {

    case AssignAddresses(addresses) =>
      val origin = sender()
      unusedAddresses match {
        case Nil => throw new Exception("The mixer has no more addresses!") //TODO: should I just get new addresses?
        case nextAddress :: rest =>
          pairedAddresses = pairedAddresses + (nextAddress -> addresses)
          unusedAddresses = rest
          origin ! AssignedDepositAddress(nextAddress)
      }

    case GetDepositAddresses =>
      val origin = sender()
      origin ! AssignedDepositAddresses(pairedAddresses.keys.toList)

    case GetDepositAndWithdrawalAddresses =>
      val origin = sender()
      origin ! DepositAndWithdrawalAddresses(pairedAddresses)
  }
}
