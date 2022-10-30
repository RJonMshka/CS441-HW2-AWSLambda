package lambda


import HelperUtils.ObtainConfigReference

import scala.jdk.CollectionConverters._
import java.util.regex.Pattern
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.typesafe.config.Config

/**
 * This class is the main entry point for lambda function
 */
class ServerlessLogProcessor extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent]{

  /**
   * The event handler for API Gateway proxy events for the lambda function
   * @param input - an API gateway event (proxy event representing incoming request data)
   * @param context - runtime context of lambda function
   * @return - a API gateway proxy response
   */
  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val lambdaLogger = context.getLogger
    lambdaLogger.log("Lambda function running")

    lambdaLogger.log("loading configurations")

    // get config file
    val lambdaConfigReference: Config = ObtainConfigReference("lambda", lambdaLogger) match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    val lambdaConfig: Config = lambdaConfigReference.getConfig("lambda")

    // get patterns from config
    val logMessagePattern = Pattern.compile(lambdaConfig.getString("logMessagePattern"))
    val stringMessagePattern = Pattern.compile(lambdaConfig.getString("stringMessagePattern"))


    val inputParams = input.getQueryStringParameters.asScala
    val responseEvent: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent

    // validate date
    if(!TimeUtil.validateDateString(inputParams("date")))
        return responseEvent
          .withStatusCode(lambdaConfig.getInt("statusCodes.INTERNAL_SERVER_ERROR"))
          .withBody(lambdaConfig.getString("badResponses.invalidDate"))

    // validate time
    if(!TimeUtil.validateTimeString(inputParams("time")))
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.INTERNAL_SERVER_ERROR"))
        .withBody(lambdaConfig.getString("badResponses.invalidTime"))

    val logDate = inputParams("date")
    val logTime = inputParams("time")
    val interval = inputParams("interval").toInt

    lambdaLogger.log(s"log time requested is $logTime")

    val endTime = TimeUtil.getIntervalEndTime(logTime, interval)
    val hashTableData = AwsUtils.getHashtableFromS3(lambdaLogger)
    val logFilesData = LogProcessingUtils.getLogFileNames(logDate, hashTableData)

    // if no hashtable data
    if(logFilesData == null) {
      lambdaLogger.log("date not matched with any data in the bucket")
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.NOT_FOUND"))
        .withBody(lambdaConfig.getString("badResponses.noFileFound"))
    }

    val filteredLogFileData = logFilesData.filter(logFileDataTuple => LogProcessingUtils.checkInterval(logTime, endTime, logFileDataTuple))

    // ----- TASK 1 -----
    // if logs not available - TASK 1
    if(filteredLogFileData.isEmpty) {
      lambdaLogger.log("logs not found in range")
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.NOT_FOUND"))
        .withBody(lambdaConfig.getString("badResponses.noLogMessagesInRange"))
    }

    val logFileToLoad = filteredLogFileData(0)._3
    val logData = AwsUtils.getDataFromS3Bucket(logFileToLoad, lambdaLogger)

    lambdaLogger.log("range exists in file")
    val rangeStartIndex = LogProcessingUtils.BinarySearchLogMessages(logTime, logData, 0, logData.length - 1)

    // if Binary search went bad for start time of interval
    if(rangeStartIndex == Int.MaxValue) {
      lambdaLogger.log("start of range not found")
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.NOT_FOUND"))
        .withBody(lambdaConfig.getString("badResponses.noLogMessagesInRange"))
    }

    lambdaLogger.log(s"range start index is: $rangeStartIndex")

    val rangeEndIndex = LogProcessingUtils.BinarySearchLogMessages(endTime, logData, rangeStartIndex, logData.length - 1)

    // if Binary search went bad for end time of interval
    if(rangeEndIndex == Int.MaxValue) {
      lambdaLogger.log("end of range not found")
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.NOT_FOUND"))
        .withBody(lambdaConfig.getString("badResponses.noLogMessagesInRange"))
    }

    lambdaLogger.log(s"range end index is: $rangeEndIndex")

    val intervalLogData = logData.slice(rangeStartIndex, rangeEndIndex)

    // ----- TASK 2 -----
    // Filter log data by matching it to Log generator pattern - TASK 2
    val filteredLogData = intervalLogData.filter(
      logMessage => {
        val logMessageMatcher = logMessagePattern.matcher(logMessage)
        logMessageMatcher.matches
        val stringMessagePatternMatcher =  stringMessagePattern.matcher(logMessageMatcher.group(5))
        stringMessagePatternMatcher.matches
      }
    )

    // If no logs matched the pattern
    if(filteredLogData.isEmpty) {
      lambdaLogger.log("no log message matched the string pattern")
      return responseEvent
        .withStatusCode(lambdaConfig.getInt("statusCodes.NOT_FOUND"))
        .withBody(lambdaConfig.getString("badResponses.noPatternMatch"))
    }

    val filteredLogDataString = filteredLogData.mkString("\n")

    // MD5 hashing
    val hashedData = LogProcessingUtils.md5Hashing(filteredLogDataString)

    // return good response
    responseEvent
      .withStatusCode(lambdaConfig.getInt("statusCodes.OK"))
      .withBody(hashedData)

  }

}
