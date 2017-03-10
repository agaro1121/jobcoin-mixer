package helpers

import scala.concurrent.duration.Duration

class JobcoinConfig(
  val url: String,
  val transactionEndpoint: String,
  val addressEndpoint: String,
  val retries: Int,
  val timeout: Duration

)
