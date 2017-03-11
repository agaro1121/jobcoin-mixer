import helpers.JobcoinConfig
import play.api.mvc.{Action, Results}
import play.api.test.WsTestClient
import play.core.server.Server
import services.JobcoinService
import play.api.routing.sird._
import scala.concurrent.duration._
import scala.language.postfixOps

object TestUtils {

  private val emptyConfig = new JobcoinConfig(
    url = "",
    transactionEndpoint = "/transactions",
    addressEndpoint = "",
    retries = 2,
    timeout = 1 minute
  )

  def withJobcoinService[T](block: JobcoinService => T): T = {
    Server.withRouter() {

      case GET(p"/transactions") =>
        Action(Results.Ok(JobcoinMockPayloads.Transactions))

      case POST(p"/transactions") =>
        Action(Results.Ok(JobcoinMockPayloads.NewTransactionSuccessResponse))

    } { implicit port =>
      WsTestClient.withClient { client =>
        block(new JobcoinService(emptyConfig, client))
      }
    }
  }

}
