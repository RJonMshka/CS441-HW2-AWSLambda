package lambda

import com.typesafe.config.{Config, ConfigFactory}

import java.security.MessageDigest
import java.util.regex.Pattern
import javax.xml.bind.DatatypeConverter
import scala.collection.mutable

object LogProcessingUtils {

  val config: Config = ConfigFactory.load().getConfig("lambda")
  val logMessagePattern: Pattern = Pattern.compile(config.getString("logMessagePattern"))

  def checkInterval(start: String, end: String, data: (String, String, String)): Boolean = {
    val firstLogTime = data._1
    val lastLogTime = data._2
    TimeUtil.getInterval(firstLogTime, start) >= 0 && TimeUtil.getInterval(end, lastLogTime) >= 0
  }

  def BinarySearchLogMessages(timeToSearch: String, data: Array[String], startIndex: Int, endIndex: Int): Int = {
    if(endIndex >= startIndex) {
      val middleIndex = (endIndex + startIndex) / 2
      val middleLogMatcher = logMessagePattern.matcher(data(middleIndex))

      if(middleLogMatcher.matches()) {
        val middleTime = middleLogMatcher.group(1)
        val timeDifference = TimeUtil.getInterval(timeToSearch, middleTime)

        if(timeDifference == 0) {
          // middle is the answer
          return middleIndex
        } else if(timeDifference > 0) {
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

  def md5Hashing(toHash: String): String = {
    val md5: MessageDigest = MessageDigest.getInstance("MD5")
    md5.update(toHash.getBytes)
    DatatypeConverter.printHexBinary(md5.digest).toUpperCase
  }

  def getTimeAndFileNameFromHTEntry(htEntry: String): (String, String, String) = {
    val splitData = htEntry.split('|')
    (splitData(0), splitData(1), splitData(2))
  }

  def getHTEntries(logFileData: String): Array[String] = {
    logFileData.split(config.getString("sameDateFilesDataDelimiter"))
  }

  def getLogFileNames(date: String, hashFileData: mutable.Map[String, String]): Array[(String, String, String)] = {
    if(hashFileData.contains(date)) {
      val logFileData = hashFileData(date)
      getHTEntries(logFileData).map(getTimeAndFileNameFromHTEntry)
    } else {
      null
    }
  }


}
