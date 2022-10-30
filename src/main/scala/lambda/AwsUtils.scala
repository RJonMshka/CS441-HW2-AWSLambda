package lambda

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.S3Object
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable

/**
 * AWS connection utilities object
 */
object AwsUtils {

  // config reference and s3 client object reference
  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build()
  val s3Config: Config = ConfigFactory.load().getConfig("s3")

  /**
   * This method gets content of log data from S3 bucket and transforms the data into an Array
   * @param logFile - path of file to read in S3 bucket
   * @param logger - Lambda logger - writes to cloudwatch log groups
   * @return Array[String] representing Array of each line of log file
   */
  def getDataFromS3Bucket(logFile: String, logger: LambdaLogger): Array[String] = {
    try {

      val logFileObject: S3Object = s3Client.getObject(s3Config.getString("bucketName"), logFile)
      val logFileContent = logFileObject.getObjectContent

      val data = scala.io.Source.fromInputStream(logFileContent).getLines().toArray
      logFileContent.close()
      data
    } catch {
      case e: Throwable => logger.log(s"Error found while accessing logs is: ${e.getMessage}")
      null
    }
  }

  /**
   * This method gets the content hashtable text file and converts it into a map
   * @param logger - lambda logger
   * @return - a hashMap representing content of hashtable file
   */
  def getHashtableFromS3(logger: LambdaLogger): mutable.Map[String, String] = {
    try {
      val hashTableFileObject: S3Object = s3Client.getObject(s3Config.getString("bucketName"), s3Config.getString("hashTableFilePath"))
      val fileContent = hashTableFileObject.getObjectContent
      val hashTable: mutable.Map[String, String] = new mutable.HashMap()
      scala.io.Source.fromInputStream(fileContent).getLines().foreach(line => {
        val lineSplit = line.split(s3Config.getString("hashTableKeyValueSeparator"))
        hashTable.put(lineSplit(0), lineSplit(1))
      })
      fileContent.close()
      hashTable
    } catch {
      case e: Throwable => logger.log(s"Error found while accessing hashtable file is: ${e.getMessage}")
        null
    }
  }

}
