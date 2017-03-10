package modules

import java.util.concurrent.TimeUnit

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import constants.Constants._
import exceptions.MissingConfigurationException
import helpers.JobcoinConfig

import scala.concurrent.duration.FiniteDuration

class JobcoinConfigProvider(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {
    val url = configuration.getString(urlPath).getOrElse(throw MissingConfigurationException(urlPath))
    val transActionEndpoint = configuration.getString(transactionsEndpointPath).getOrElse(throw MissingConfigurationException(transactionsEndpointPath))
    val addressEndpoint = configuration.getString(addressesEndpointPath).getOrElse(throw MissingConfigurationException(addressesEndpointPath))
    val retries = configuration.getInt(jobcoinRetries).getOrElse(throw MissingConfigurationException(jobcoinRetries))
    val timeout = FiniteDuration(configuration.getMilliseconds(jobcoinTimeout).get, TimeUnit.MILLISECONDS)

    bind(classOf[JobcoinConfig])
      .annotatedWith(Names.named(jobcoinConfig))
      .toInstance(new JobcoinConfig(url, transActionEndpoint, addressEndpoint, retries, timeout))

  }

}
