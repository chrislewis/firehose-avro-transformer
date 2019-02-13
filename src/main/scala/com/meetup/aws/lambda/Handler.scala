package com.meetup.aws.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import java.util
import java.util.Base64
import java.util.UUID.randomUUID

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicSessionCredentials}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.securitytoken.model.{AssumeRoleRequest, Credentials, GetSessionTokenRequest, GetSessionTokenResult}
import com.amazonaws.services.securitytoken.{AWSSecurityTokenService, AWSSecurityTokenServiceClientBuilder}
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.meetup.aws.lambda.model._
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}

import scala.collection.mutable
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class Handler extends RequestHandler[Request, Response] {

  val clientRegion: String = sys.env.getOrElse("REGION", "us-east-1")
  val roleARN: String = sys.env.getOrElse("ROLE_ARN", "arn:aws:iam::212646169882:role/firehose-avro-transformer")
  val roleSessionName: String = randomUUID().toString
  val stsClient : AWSSecurityTokenService = AWSSecurityTokenServiceClientBuilder.standard.withRegion(clientRegion).build()

  // Assume the IAM role. Note that you cannot assume the role of an AWS root account;
  // Amazon S3 will deny access. You must use credentials for an IAM user or an IAM role.
  val roleRequest: AssumeRoleRequest = new AssumeRoleRequest().withRoleArn(roleARN).withRoleSessionName(roleSessionName)
  stsClient.assumeRole(roleRequest)

  // Start a session.
  val getSessionTokenRequest = new GetSessionTokenRequest
  // The duration can be set to more than 3600 seconds only if temporary
  // credentials are requested by an IAM user rather than an account owner.
  getSessionTokenRequest.setDurationSeconds(3600)
  val sessionTokenResult: GetSessionTokenResult = stsClient.getSessionToken (getSessionTokenRequest)
  val sessionCredentials: Credentials = sessionTokenResult.getCredentials

  // Package the temporary security credentials as a BasicSessionCredentials object // Package the temporary security credentials as a BasicSessionCredentials object
  // for an Amazon S3 client object to use.
  val basicSessionCredentials = new BasicSessionCredentials(sessionCredentials.getAccessKeyId, sessionCredentials.getSecretAccessKey, sessionCredentials.getSessionToken)
  val s3: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(basicSessionCredentials))
    .withRegion(clientRegion).build()

  val targetBucket : String = Option(sys.env("TARGET_BUCKET")).getOrElse("com.meetup.firehose")
  val targetPrefix : String = Option(sys.env("TARGET_PREFIX")).getOrElse("avro/")

	def handleRequest(event: Request, context: Context): Response = {
    val decoder : Base64.Decoder = Base64.getDecoder
    val mapper : ObjectMapper  = new ObjectMapper()
    val result: util.LinkedList[ProcessedRecord] = new util.LinkedList[ProcessedRecord]()
    val avros: mutable.MutableList[AvroRecord] = new mutable.MutableList[AvroRecord]()

    event.records.foreach(entry => {
      Try {
        mapper.readValue[AvroRecord](decoder.decode(entry.data), classOf[AvroRecord])
      } match {
        case Success(record) =>
          avros+=record
          result.add(ProcessedRecord(entry.recordId, "Ok", entry.data))
        case Failure(e) =>
          result.add(ProcessedRecord(entry.recordId, "ProcessingFailed", entry.data + ":" + e.getMessage))
      }
    })

    val mapped: Map[String, mutable.MutableList[AvroRecord]] = avros.groupBy(_.schema)

    // for each map entry, process the corresponding array of records
    mapped.foreach(p => {
      val records = p._2

      getSchema(p._1) match {
        case Success(schema) =>
          val dataFileWriter = new DataFileWriter[GenericRecord](new GenericDatumWriter[GenericRecord](schema))
          val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
          dataFileWriter.create(schema, bos)
          records.foreach(entry => {
            dataFileWriter.appendEncoded(ByteBuffer.wrap(decoder.decode(entry.record)))
          })
          dataFileWriter.close()

          writeObject(targetBucket,
            targetPrefix
              + deCamelCase(schema.getName)
              + "/date="
              + records(0).date
              + "/av_version="
              + schema.getDoc
              + "/avro-"
              + System.currentTimeMillis() / 1000
              + "_" + randomUUID, bos)
        case Failure(e) =>
          println ("ERROR: schema not present" + p._1)
      }


    })

		Response(result)
	}

  private def writeObject(bucket:String, key:String, bos: ByteArrayOutputStream): Unit = {
    val data = bos.toByteArray
    val omd = new ObjectMetadata()
    omd.setContentType("avro/binary")
    omd.setContentLength(data.length)
    s3.putObject(bucket, key, new ByteArrayInputStream(bos.toByteArray), omd)
    println (s"Written $key")
  }

  private def getSchema(url: String): Try[Schema] = {
    val path: String = url.replace("gs://meetup-logs/", "")
    Try {
      new Schema.Parser().parse(Resources.toString(Resources.getResource(path), Charsets.UTF_8))
    }
 }

  val CaseBoundary: Regex = """([a-z0-9_])([A-Z])""".r
  def deCamelCase(name: String): String = {
    CaseBoundary.replaceSomeIn (name, m => Some (m.group (1) + "_" + m.group (2) ) ).toLowerCase
  }
}