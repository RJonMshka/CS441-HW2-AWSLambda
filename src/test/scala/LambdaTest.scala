import com.amazonaws.services.lambda.runtime.LambdaLogger
import lambda.{LogProcessingUtils, TimeUtil}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers._

import scala.collection.mutable

class LambdaTest extends AnyFunSpec {

  val dummyLogger: LambdaLogger = new LambdaLogger {
    override def log(message: String): Unit = ()
    override def log(message: Array[Byte]): Unit = ()
  }

  describe("Testing of Log Processor AWS Lambda Function") {

    describe("Testing of Time utils") {

      it("Should find the difference between two time strings") {
        val time1 = "14:27:37.441"
        val time2 = "14:27:47.441"

        TimeUtil.getInterval(time1, time2) shouldBe 10000
      }

      it("Should find zero difference between two same time strings") {
        val time1 = "14:27:37.441"
        val time2 = "14:27:37.441"

        TimeUtil.getInterval(time1, time2) shouldBe 0
      }

      it("Should find the negative difference between two time strings") {
        val time1 = "14:27:37.441"
        val time2 = "14:27:27.441"

        TimeUtil.getInterval(time1, time2) shouldBe -10000
      }

      it("should get the return a new string with added time interval") {
        val time1 = "14:27:27.441"
        val intervalInSeconds = 20

        TimeUtil.getIntervalEndTime(time1, intervalInSeconds) shouldBe "14:27:47.441"
      }

      it("should validate a good time string") {
        val time1 = "14:27:27.441"
        TimeUtil.validateTimeString(time1) shouldBe true
      }

      it("should validate a bad time string") {
        val time1 = "14:27"
        TimeUtil.validateTimeString(time1) shouldBe false
      }

      it("should validate a good date string") {
        val date1 = "2022-10-25"
        TimeUtil.validateDateString(date1) shouldBe true
      }

      it("should validate a bad date string") {
        val date1 = "2022/10/25"
        TimeUtil.validateDateString(date1) shouldBe false
      }
    }

    describe("Testing of log processing utils") {

      it("Should test MD5 hashing") {
        val str = "1234567890"
        // https://www.md5hashgenerator.com/ used for output generation
        val md5hash = "e807f1fcf82d132f9bb018ca6738a19f".toUpperCase

        LogProcessingUtils.md5Hashing(str) shouldBe md5hash
      }

      it("should check interval in valid data") {
        val startTime = "14:27:27.441"
        val endTime = "14:27:37.441"

        val data = ("14:00:00.000", "15:00:00.000", "logFileName")

        LogProcessingUtils.checkInterval(startTime, endTime, data) shouldBe true
      }

      it("should check interval in invalid data") {
        val startTime = "14:27:27.441"
        val endTime = "14:27:37.441"

        val data = ("14:00:00.000", "14:20:00.000", "logFileName")

        LogProcessingUtils.checkInterval(startTime, endTime, data) shouldBe false
      }

      it("should calculate closest log message index based on timestamp by Binary Search") {
        val testArray: Array[String] = Array("21:45:13.670 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - Hx-kE.'TcvA6Pge?OPbcxC4WuL6fB8ubg1ae0O9od9#U1T&x7j)_a2-L]zp*nAf={",
          "21:45:13.696 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - _*)#TVC6uN6fbg1T7vI6tI5kbg27nF]-C",
          "21:45:13.755 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - >KY,;%6Fbe1ae1Z7uae0X8kbg0Y5qN7vO9f`|HavZ,XU",
          "21:45:13.789 [scala-execution-context-global-17] WARN  HelperUtils.Parameters$ - KzFZh2h,^wRbF~pg][fuZtOM&_pxcf1X6raf0B9tcf2C8ujuF[phLbP6;@G=TvY&-M_Mv=)P7s",
          "21:45:13.842 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - `a1fx_BkA7R1cjHW.IJbf1cg2af0be3bg3cf0L7iS6vbe1Ph*|,H*@1l:-2YQmX18",
          "21:45:13.891 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - ^j4\":sR.UgVQ`A8:LE7kbg1N6lF8kR5tbg2D7i'?I,;/NBlo=([oVd?",
          "21:45:13.920 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - .?C|_Nt;Px@W^`,cyIo*{8'+Ccg2cg0S7lae0X6iY^?X/w*yn$8oA3UBDhj~kJi)V)",
          "21:45:13.962 [scala-execution-context-global-17] WARN  HelperUtils.Parameters$ - qjw\f^`73^?zYqT#3;\"e5jI`FT9ae1bg1G7wH7rP7hbf0I7sUJQ?GW^N/J<hg_\"0s~;tH<a\"SW,;h",
          "21:45:13.993 [scala-execution-context-global-17] ERROR HelperUtils.Parameters$ - t(>Jx)fc6${4B$OY&_H`9M,U7oR8iaf3cf0bf3P5maf0ce1-K\"rRsGSp@X$+P{q[g\"T6O6",
          "21:45:14.037 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - tVw+^G4'<|)FWU6gK8oce3cg1B7iL9mbe3E)Lbh6J%qu8Q!",
          "21:45:14.063 [scala-execution-context-global-17] WARN  HelperUtils.Parameters$ - $&D3g7aLdOg*iUw@jHyKS$X7wbe3W7gV6qT5vy]gb[8a>,C$tr^haT:b8e|c",
          "21:45:14.091 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - ~}A|tFH$h7:9%Rq`?Y<!k3yNbf1F7tcg3W8mG5iag0cf3O7t&&~C*#0)#xO4,{Nt&aZctgr",
          "21:45:14.134 [scala-execution-context-global-17] INFO  HelperUtils.Parameters$ - vr!c/9AXiC&3Js0Fx%(dHI}FZdPj*ag1M8jU8fU6vcf0~TMaL`gp(4_G9bebq;6ZbFZ;*F]5E")

        val timeToTest = "21:45:13.900"

        LogProcessingUtils.BinarySearchLogMessages(timeToTest, testArray, 0, testArray.length - 1) shouldBe 6
      }

      it("should test getTimeAndFileNameFromHTEntry - given the string with start, end and fileName returns each item in a Tuple3") {
        val data = "17:00:44.611|17:00:58.543|log/LogFileGenerator.2022-10-29.0.log"

        LogProcessingUtils.getTimeAndFileNameFromHTEntry(data)._1 shouldBe "17:00:44.611"
        LogProcessingUtils.getTimeAndFileNameFromHTEntry(data)._2 shouldBe "17:00:58.543"
        LogProcessingUtils.getTimeAndFileNameFromHTEntry(data)._3 shouldBe "log/LogFileGenerator.2022-10-29.0.log"
      }

      it("should test getHTEntries - given a single log date string, should return data of every logFile in an array of string") {
        val data = "15:10:24.643|15:10:38.792|log/LogFileGenerator.2022-10-29.0.log,15:10:38.806|15:10:41.318|log/LogFileGenerator.2022-10-29.1.log,"

        val result = LogProcessingUtils.getHTEntries(data)

        result(0) shouldBe "15:10:24.643|15:10:38.792|log/LogFileGenerator.2022-10-29.0.log"
        result(1) shouldBe "15:10:38.806|15:10:41.318|log/LogFileGenerator.2022-10-29.1.log"
      }

      it("should test getLogFileNames - given a hashMap and a particular date, check if that date is key in hashmap and if it is, return its attributes in Array of Tuple3") {
        val testHashMap = new mutable.HashMap[String, String]()

        testHashMap.put("2022-10-29","15:10:24.643|15:10:38.792|log/LogFileGenerator.2022-10-29.0.log,15:10:38.806|15:10:41.318|log/LogFileGenerator.2022-10-29.1.log,")
        testHashMap.put("2022-10-28","14:27:22.436|14:27:37.189|log/LogFileGenerator.2022-10-28.0.log,14:27:37.234|14:27:40.031|log/LogFileGenerator.2022-10-28.1.log,")

        val dateToTest = "2022-10-29"
        val result = LogProcessingUtils.getLogFileNames(dateToTest, testHashMap)

        result(0)._1 shouldBe "15:10:24.643"
        result(0)._2 shouldBe "15:10:38.792"
        result(0)._3 shouldBe "log/LogFileGenerator.2022-10-29.0.log"

        result(1)._1 shouldBe "15:10:38.806"
        result(1)._2 shouldBe "15:10:41.318"
        result(1)._3 shouldBe "log/LogFileGenerator.2022-10-29.1.log"
      }

      it("should test getLogFileNames - should return null if given invalid date") {
        val testHashMap = new mutable.HashMap[String, String]()

        testHashMap.put("2022-10-29","15:10:24.643|15:10:38.792|log/LogFileGenerator.2022-10-29.0.log,15:10:38.806|15:10:41.318|log/LogFileGenerator.2022-10-29.1.log,")
        testHashMap.put("2022-10-28","14:27:22.436|14:27:37.189|log/LogFileGenerator.2022-10-28.0.log,14:27:37.234|14:27:40.031|log/LogFileGenerator.2022-10-28.1.log,")

        val dateToTest = "2022-10-30"
        val result = LogProcessingUtils.getLogFileNames(dateToTest, testHashMap)

        result shouldBe null
      }

    }
  }

}
