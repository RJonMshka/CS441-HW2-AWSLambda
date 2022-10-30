package lambda

import com.typesafe.config.{Config, ConfigFactory}

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

object TimeUtil {
  // format to match the log message time
  val config: Config = ConfigFactory.load().getConfig("lambda")
  val logTimeFormatter: SimpleDateFormat = new SimpleDateFormat(config.getString("logTimeFormatter"))
  val datePattern: Pattern = Pattern.compile(config.getString("datePattern"))
  val timePattern: Pattern = Pattern.compile(config.getString("timePattern"))
  val defaultDate: String = config.getString("defaultDate")
  val millisecondsInSecond = 1000

  def getInterval(s1: String, s2: String): Long = logTimeFormatter.parse(s2).getTime - logTimeFormatter.parse(s1).getTime

  def getIntervalEndTime(s1: String, timeIntervalInSeconds: Int): String = {
    val firstTime = logTimeFormatter.parse(s1).getTime
    val secondTime = firstTime + (timeIntervalInSeconds * millisecondsInSecond)

    logTimeFormatter.format(new Date(secondTime))
  }

  def validateTimeString(timeString: String): Boolean = timePattern.matcher(timeString).matches

  def validateDateString(dateString: String): Boolean = datePattern.matcher(dateString).matches

}
