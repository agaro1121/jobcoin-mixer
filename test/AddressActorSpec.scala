import actors.AddressActor
import actors.AddressActor._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import models.Address
import org.scalatest._

class AddressActorSpec
  extends TestKit(ActorSystem("TestActorSystem"))
  with WordSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Address Actor" should {
    "return a deposit address when supplied with a list of withdrawal addresses" in {
      val addressStore = system.actorOf(AddressActor.props)

      addressStore ! AddressActor.RegisterUserWithdrawalAddresses(List(Address("Address1")))
      expectMsgType[RegisteredDepositAddress]
    }

    "return registered deposit addresses" in {
      val addressStore = system.actorOf(AddressActor.props)

      addressStore ! AddressActor.RegisterUserWithdrawalAddresses(List(Address("Address1")))
      expectMsgType[RegisteredDepositAddress]

      addressStore ! GetRegisteredDepositAddresses
      expectMsgPF() {
        case RegisteredDepositAddresses(addresses) => addresses.size shouldBe 1
      }

      addressStore ! GetDepositAndWithdrawalAddresses
      expectMsgPF() {
        case DepositAndWithdrawalAddresses(addresses) =>
          addresses.keys.size shouldBe 1
          addresses.values.size shouldBe 1
      }
    }
  }

}
