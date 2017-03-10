package exceptions

case class MissingConfigurationException(s: String) extends RuntimeException(s)