import java.time.Instant

import actors.{AddressActor, TransactionActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import models.Address
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import TestUtils._
import actors.TransactionActor.PollForTransactions

class TransactionActorSpec
  extends TestKit(ActorSystem("TestActorSystem"))
  with WordSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll {

  val addressStore: ActorRef = system.actorOf(AddressActor.props)

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  override protected def beforeAll(): Unit = {
    addressStore ! AddressActor.RegisterUserWithdrawalAddresses(List(Address("SecondAddress")))
  }

  "Transaction Actor" should {
    "do stuff" in withJobcoinService { service =>
      val transactionActor = TestActorRef[TransactionActor](new TransactionActor(service, addressStore))

      transactionActor ! PollForTransactions
      transactionActor.underlyingActor.allRelevantTransactions.isEmpty shouldBe true
      transactionActor.underlyingActor.latestTimestamp shouldBe Instant.EPOCH
      transactionActor.underlyingActor.depositAddressAndBalance.size shouldBe 0

    }
  }

}
