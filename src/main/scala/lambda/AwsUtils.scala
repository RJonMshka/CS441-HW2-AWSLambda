package lambda

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.S3Object
import scala.collection.mutable

object AwsUtils {

  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build()

  @throws[Throwable]
  def getDataFromS3Bucket(logFile: String, logger: LambdaLogger): Array[String] = {
    try {

      val logFileObject: S3Object = s3Client.getObject("rajat-cs441-hw2", logFile)
      val logFileContent = logFileObject.getObjectContent

      val data = scala.io.Source.fromInputStream(logFileContent).getLines().toArray
      logFileContent.close
      data
    } catch {
      case e: Throwable => logger.log(s"error found while accessing logs is: ${e.getMessage}")
      null
    }
  }

  def getHashtableFromS3(logger: LambdaLogger): mutable.Map[String, String] = {
    try {
      val hashTableFileObject: S3Object = s3Client.getObject("rajat-cs441-hw2", "hashtable/hashtable.txt")
      val fileContent = hashTableFileObject.getObjectContent
      val hashTable: mutable.Map[String, String] = new mutable.HashMap()
      scala.io.Source.fromInputStream(fileContent).getLines().foreach(line => {
        val lineSplit = line.split("->")
        hashTable.put(lineSplit(0), lineSplit(1))
      })
      fileContent.close
      hashTable
    } catch {
      case e: Throwable => logger.log(s"error found while accessing hashtable file is: ${e.getMessage}")
        null
    }
  }

}
