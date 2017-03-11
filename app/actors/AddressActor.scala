package actors

import akka.actor.{Actor, Props}
import models.Address

import scala.collection.immutable.Seq

object AddressActor {
  case class RegisterUserWithdrawalAddresses(addresses: List[Address])
  case object GetRegisteredDepositAddresses
  case object GetDepositAndWithdrawalAddresses
  case class RegisteredDepositAddresses(addresses: List[Address])
  case class RegisteredDepositAddress(address: Address)
  case class DepositAndWithdrawalAddresses(addresses: Map[Address, List[Address]])

  def props: Props = Props[AddressActor]
}

class AddressActor extends Actor {

  import AddressActor._

  var unusedDepositAddresses: Seq[Address] = List.fill(1000)(Address(java.util.UUID.randomUUID.toString))
  var registeredDepositAddresses: Map[Address, List[Address]] = Map.empty

  override def receive: Receive = {

    case RegisterUserWithdrawalAddresses(addresses) =>
      val origin = sender()
      unusedDepositAddresses match {
        case Nil => throw new Exception("The mixer has no more addresses!")
        case nextAddress :: rest =>
          registeredDepositAddresses = registeredDepositAddresses + (nextAddress -> addresses)
          unusedDepositAddresses = rest
          origin ! RegisteredDepositAddress(nextAddress)
      }

    case GetRegisteredDepositAddresses =>
      val origin = sender()
      origin ! RegisteredDepositAddresses(registeredDepositAddresses.keys.toList)

    case GetDepositAndWithdrawalAddresses =>
      val origin = sender()
      origin ! DepositAndWithdrawalAddresses(registeredDepositAddresses)
  }
}
