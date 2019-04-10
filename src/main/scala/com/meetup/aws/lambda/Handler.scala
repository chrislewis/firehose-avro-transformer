package com.meetup.aws.lambda

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.model.ObjectMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.meetup.aws.lambda.model._
import com.meetup.aws.lambda.util.S3Client
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID.randomUUID

import org.apache.avro.io.DecoderFactory

import scala.collection.mutable
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


class Handler extends RequestHandler[Request, Response] {
  
  val targetBucket : String = Option(sys.env("TARGET_BUCKET")).getOrElse("com.meetup.firehose")
  val targetPrefix : String = Option(sys.env("TARGET_PREFIX")).getOrElse("avro/")
  val s3: S3Client = S3Client()
  val decoder : Base64.Decoder = Base64.getDecoder

	def handleRequest(event: Request, context: Context): Response = {
    val mapper : ObjectMapper  = new ObjectMapper()
    val result: java.util.LinkedList[ProcessedRecord] = new java.util.LinkedList[ProcessedRecord]()
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
            doAppend(dataFileWriter, entry, schema)
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

  protected def doAppend(writer: DataFileWriter[GenericRecord], record: AvroRecord, schema: Schema) = {
    record.contentType match {
      case "application/json" =>
        val bin = new ByteArrayInputStream(record.record.getBytes("UTF-8"))
        val jsonDecoder = DecoderFactory.get.jsonDecoder(schema, bin)
        val datumReader = new GenericDatumReader[GenericRecord](schema)
        val datum = datumReader.read(null, jsonDecoder)
        writer.append(datum)
      case _ => writer.appendEncoded(ByteBuffer.wrap(decoder.decode(record.record)))
    }
  }

  private def writeObject(bucket:String, key:String, bos: ByteArrayOutputStream): Unit = {
    val data = bos.toByteArray
    val omd = new ObjectMetadata()
    omd.setContentType("avro/binary")
    omd.setContentLength(data.length)
    s3.client.putObject(bucket, key, new ByteArrayInputStream(bos.toByteArray), omd)
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
