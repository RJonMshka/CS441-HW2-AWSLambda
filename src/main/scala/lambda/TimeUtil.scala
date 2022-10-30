package lambda

import com.typesafe.config.{Config, ConfigFactory}

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

/**
 * Object for time and date specific utilities
 */
object TimeUtil {
  // format to match the log message time
  val config: Config = ConfigFactory.load().getConfig("lambda")
  val logTimeFormatter: SimpleDateFormat = new SimpleDateFormat(config.getString("logTimeFormatter"))
  val datePattern: Pattern = Pattern.compile(config.getString("datePattern"))
  val timePattern: Pattern = Pattern.compile(config.getString("timePattern"))
  val defaultDate: String = config.getString("defaultDate")
  val millisecondsInSecond = 1000

  /**
   * This method calculate the time difference in milliseconds between two time string in "HH:mm:ss.SSS" format
   * @param s1 - first time string
   * @param s2 - second time string
   * @return - time difference in milliseconds
   */
  def getInterval(s1: String, s2: String): Long = logTimeFormatter.parse(s2).getTime - logTimeFormatter.parse(s1).getTime

  /**
   * This method returns the time string with original string added with interval in seconds
   * @param s1 - original time string
   * @param timeIntervalInSeconds - time interval in seconds
   * @return - end time string of interval
   */
  def getIntervalEndTime(s1: String, timeIntervalInSeconds: Int): String = {
    val firstTime = logTimeFormatter.parse(s1).getTime
    val secondTime = firstTime + (timeIntervalInSeconds * millisecondsInSecond)

    logTimeFormatter.format(new Date(secondTime))
  }

  /**
   * This method validates the time string
   * @param timeString - time string to validate
   * @return - boolean showing result of validation
   */
  def validateTimeString(timeString: String): Boolean = timePattern.matcher(timeString).matches

  /**
   * This method validates the date string
   * @param dateString - date string to validate
   * @return - boolean showing result of validation
   */
  def validateDateString(dateString: String): Boolean = datePattern.matcher(dateString).matches

}
