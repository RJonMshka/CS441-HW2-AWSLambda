package lambda

import com.typesafe.config.{Config, ConfigFactory}

import java.security.MessageDigest
import java.util.regex.Pattern
import javax.xml.bind.DatatypeConverter
import scala.collection.mutable


/**
 * This object exposes utility methods for processing hashtable and log data
 */
object LogProcessingUtils {

  // config reference
  val config: Config = ConfigFactory.load().getConfig("lambda")
  val logMessagePattern: Pattern = Pattern.compile(config.getString("logMessagePattern"))

  /**
   * This method checks if the start and end time given is in log range data
   * @param start - start time that needs to be checked
   * @param end - end time that needs to be checked
   * @param data - data of log file represented in a tuple3, (start, end, logFileName)
   * @return - a boolean representing if the given start and end time is in log file range
   */
  def checkInterval(start: String, end: String, data: (String, String, String)): Boolean = {
    val firstLogTime = data._1
    val lastLogTime = data._2
    TimeUtil.getInterval(firstLogTime, start) >= 0 && TimeUtil.getInterval(end, lastLogTime) >= 0
  }

  /**
   * This method performs binary search on the log data provided, given the start and end task
   * @param timeToSearch - time to search
   * @param data - data of log file
   * @param startIndex - start index for search
   * @param endIndex - end index for search
   * @return - index of the closed line with timestamp matching the timeToSearch param
   */
  def BinarySearchLogMessages(timeToSearch: String, data: Array[String], startIndex: Int, endIndex: Int): Int = {
    if(endIndex >= startIndex) {
      val middleIndex = (endIndex + startIndex) / 2
      val middleLogMatcher = logMessagePattern.matcher(data(middleIndex))

      // match each log with log message pattern
      if(middleLogMatcher.matches()) {
        val middleTime = middleLogMatcher.group(1)
        val timeDifference = TimeUtil.getInterval(timeToSearch, middleTime)

        if(timeDifference == 0) {
          // perfect match
          return middleIndex
        } else if(timeDifference > 0) {
          // if closest to middle
          val beforeMiddleIndex = middleIndex - 1
          val beforeMiddleLogMatcher = logMessagePattern.matcher(data(beforeMiddleIndex))
          if(beforeMiddleLogMatcher.matches()) {
            if(TimeUtil.getInterval(timeToSearch, beforeMiddleLogMatcher.group(1)) < 0) {
              // this means that the one above middle is less than the time to search
              return middleIndex
            } else {
              return BinarySearchLogMessages(timeToSearch, data, startIndex, middleIndex - 1)
            }
          }
        } else {
          // if closest to middle
          val afterMiddleIndex = middleIndex + 1
          val afterMiddleLogMatcher = logMessagePattern.matcher(data(afterMiddleIndex))
          if(afterMiddleLogMatcher.matches()) {
            if(TimeUtil.getInterval(timeToSearch, afterMiddleLogMatcher.group(1)) > 0) {
              return afterMiddleIndex
            } else {
              return BinarySearchLogMessages(timeToSearch, data, middleIndex + 1, endIndex)
            }
          }
        }
      }
    }
    Int.MaxValue
  }

  /**
   * This method performs MD5 hashing on a string
   * @param toHash - a string that needs to be hashed with MD5 algorithm
   * @return - a MD5 hash of string data
   */
  def md5Hashing(toHash: String): String = {
    val md5: MessageDigest = MessageDigest.getInstance("MD5")
    md5.update(toHash.getBytes)
    DatatypeConverter.printHexBinary(md5.digest).toUpperCase
  }

  /**
   * This method de-structures a hashtable entry
   * @param htEntry - a string data represents single log file in hashtable
   * @return - a Tuple3 (startTime, endTime, logFileName)
   */
  def getTimeAndFileNameFromHTEntry(htEntry: String): (String, String, String) = {
    val splitData = htEntry.split('|')
    (splitData(0), splitData(1), splitData(2))
  }

  /**
   * This method converts a single hashtable entry (representing one or multiple log files for a particular date) into and array (each entry in array will be a data for each log file)
   * @param logFileData - a string data representing each log file data
   * @return - array (each entry in array will be a data for each log file)
   */
  def getHTEntries(logFileData: String): Array[String] = {
    logFileData.split(config.getString("sameDateFilesDataDelimiter"))
  }

  /**
   * This method convert hashMap data into de-structured data into an array
   * @param date - date to filter data from hashMap
   * @param hashFileData - a hashMap representing hashtable data
   * @return - an array of Tuple3 (start, end, fileName) or null
   */
  def getLogFileNames(date: String, hashFileData: mutable.Map[String, String]): Array[(String, String, String)] = {
    if(hashFileData.contains(date)) {
      val logFileData = hashFileData(date)
      getHTEntries(logFileData).map(getTimeAndFileNameFromHTEntry)
    } else {
      null
    }
  }


}
