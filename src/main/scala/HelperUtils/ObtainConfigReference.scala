package HelperUtils

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success, Try}

/**
 * Loads the config file, validates for existence and then returns the config
 */
object ObtainConfigReference {
  private val config = ConfigFactory.load()

  private def ValidateConfig(confEntry: String, logger: LambdaLogger): Boolean = Try(config.getConfig(confEntry)) match {
    case Failure(exception) => logger.log(s"Failed to retrieve config entry $confEntry for reason $exception"); false
    case Success(_) => true
  }

  def apply(confEntry: String, logger: LambdaLogger): Option[Config] = if (ValidateConfig(confEntry, logger)) {Some(config)} else None
}
