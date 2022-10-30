package lambda

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

object TimeUtil {
  // format to match the log message time
  val logTimeFormatter: SimpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS")
  val datePattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}")
  val timePattern = Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}")
  val defaultDate = "2022-10-25"

  def getInterval(s1: String, s2: String): Long = logTimeFormatter.parse(s2).getTime - logTimeFormatter.parse(s1).getTime

  def getIntervalEndTime(s1: String, timeIntervalInSeconds: Int): String = {
    val firstTime = logTimeFormatter.parse(s1).getTime
    val secondTime = firstTime + (timeIntervalInSeconds * 1000)

    logTimeFormatter.format(new Date(secondTime))
  }

  def validateTimeString(timeString: String): Boolean = timePattern.matcher(timeString).matches

  def validateDateString(dateString: String): Boolean = datePattern.matcher(dateString).matches

}
