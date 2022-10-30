package lambda


import scala.jdk.CollectionConverters._
import java.util.regex.Pattern
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

class ServerlessLogProcessor extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent]{
  val logMessagePattern = Pattern.compile("(.+)\\s\\[(.+)\\]\\s+(WARN|ERROR|DEBUG|INFO)\\s+(.+)\\s+-\\s+(.+)\\s*")
  val stringMessagePattern = Pattern.compile("(.*)([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}(.*)")

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val lambdaLogger = context.getLogger
    lambdaLogger.log("Lambda function running")


    val inputParams = input.getQueryStringParameters.asScala

    val responseEvent: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent

    if(!TimeUtil.validateDateString(inputParams.get("date").get))
        responseEvent.withStatusCode(500).withBody("Invalid Date String")


    if(!TimeUtil.validateTimeString(inputParams.get("time").get))
      responseEvent.withStatusCode(500).withBody("Invalid Time String")

    val logDate = inputParams.get("date").get
    val logTime = inputParams.get("time").get
    val interval = inputParams.get("interval").get.toInt

    lambdaLogger.log(s"log time requested is ${logTime}")

    val endTime = TimeUtil.getIntervalEndTime(logTime, interval)

    val hashTableData = AwsUtils.getHashtableFromS3(lambdaLogger)

    val logFilesData = LogProcessingUtils.getLogFileNames(logDate, hashTableData, lambdaLogger)

    if(logFilesData == null) {
      lambdaLogger.log("date not matched with any data in the bucket")
      return responseEvent.withStatusCode(404).withBody("Log Messages for this particular date are not available")
    }

    val filteredLogFileData = logFilesData.filter(logFileDataTuple => LogProcessingUtils.checkInterval(logTime, endTime, logFileDataTuple, lambdaLogger))

    if(filteredLogFileData.isEmpty) {
      lambdaLogger.log("logs not found in range")
      return responseEvent.withStatusCode(404).withBody("Log Messages are not available in the given interval")
    }

    val logFileToLoad = filteredLogFileData(0)._3

    val logData = AwsUtils.getDataFromS3Bucket(logFileToLoad, lambdaLogger)

    lambdaLogger.log("range exists in file")
    val rangeStartIndex = LogProcessingUtils.BinarySearchLogMessages(logTime, logData, 0, logData.length - 1)

    if(rangeStartIndex == Int.MaxValue) {
      lambdaLogger.log("start of range not found")
      return responseEvent.withStatusCode(404).withBody("Log Messages are not available in the given interval")
    }

    lambdaLogger.log(s"range start index is: ${rangeStartIndex}")

    val rangeEndIndex = LogProcessingUtils.BinarySearchLogMessages(endTime, logData, rangeStartIndex, logData.length - 1)

    if(rangeEndIndex == Int.MaxValue) {
      lambdaLogger.log("end of range not found")
      return responseEvent.withStatusCode(404).withBody("Log Messages are not available in the given interval")
    }

    lambdaLogger.log(s"range end index is: ${rangeEndIndex}")

    val intervalLogData = logData.slice(rangeStartIndex, rangeEndIndex)

    val filteredLogData = intervalLogData.filter(
      (logMessage) => {
        val logMessageMatcher = logMessagePattern.matcher(logMessage)
        logMessageMatcher.matches
        val stringMessagePatternMatcher =  stringMessagePattern.matcher(logMessageMatcher.group(5))
        stringMessagePatternMatcher.matches
      }
    )

    val filteredLogDataString = filteredLogData.mkString("\n")

    val hashedData = LogProcessingUtils.md5Hashing(filteredLogDataString)

    return responseEvent.withStatusCode(200).withBody(hashedData)

  }

}
