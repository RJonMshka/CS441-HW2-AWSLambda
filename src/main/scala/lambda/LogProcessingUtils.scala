package lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import sun.security.provider.MD5

import java.security.MessageDigest
import java.util.regex.Pattern
import javax.xml.bind.DatatypeConverter
import scala.collection.mutable

object LogProcessingUtils {

  val logMessagePattern = Pattern.compile("(.+)\\s\\[(.+)\\]\\s+(WARN|ERROR|DEBUG|INFO)\\s+(.+)\\s+-\\s+(.+)\\s*")

  def checkInterval(start: String, end: String, data: (String, String, String), logger: LambdaLogger): Boolean = {
    val firstLogTime = data._1
    val lastLogTime = data._2

    TimeUtil.getInterval(firstLogTime, start) >= 0 && TimeUtil.getInterval(end, lastLogTime) >= 0
  }

  def BinarySearchLogMessages(timeToSearch: String, data: Array[String], startIndex: Int, endIndex: Int): Int = {
    val startLog = data(startIndex)
    val endLog = data(endIndex)

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
    return Int.MaxValue

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

  def getHTEntries(logFileData: String): Array[(String, String, String)] = {
    val logFilesArray = logFileData.split(",")
    val newArray = logFilesArray.slice(0, logFilesArray.length - 2)
    logFilesArray.map(getTimeAndFileNameFromHTEntry)
  }

  def getLogFileNames(date: String, hashFileData: mutable.Map[String, String], logger: LambdaLogger): Array[(String, String, String)] = {
    if(hashFileData.contains(date)) {
      val logFileData = hashFileData.get(date).get
      getHTEntries(logFileData)
    } else {
      null
    }
  }

//  def main(args: Array[String]): Unit = {
//    val testmap = new mutable.HashMap[String, String]()
//
//    testmap.put("2022-10-29","15:10:24.643|15:10:38.792|log/LogFileGenerator.2022-10-29.0.log,15:10:38.806|15:10:41.318|log/LogFileGenerator.2022-10-29.1.log,")
//    testmap.put("2022-10-28","14:27:22.436|14:27:37.189|log/LogFileGenerator.2022-10-28.0.log,14:27:37.234|14:27:40.031|log/LogFileGenerator.2022-10-28.1.log,")
//
//    val lf = getLogFileNames("2022-10-29", testmap, new LambdaLogger {
//      override def log(message: String): Unit = ???
//
//      override def log(message: Array[Byte]): Unit = ???
//    })
//
//    lf.foreach(item =>
//      println(s"${item._1}, ${item._2}, ${item._3}")
//    )
//  }


}
