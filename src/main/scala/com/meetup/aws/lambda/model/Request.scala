package com.meetup.aws.lambda.model

import scala.beans.BeanProperty

/*
Structure of a FireHose request
{
  "records": [
    {
      "recordId": "49546986683135544286507457936321625675700192471156785154",
      "data": "SGVsbG8sIHRoaXMgaXMgYSB0ZXN0IDEyMy4=",
      "approximateArrivalTimestamp": "2012-04-23T18:25:43.511Z",
      "kinesisRecordMetadata": {
        "shardId": "shardId-000000000000",
        "partitionKey": "4d1ad2b9-24f8-4b9d-a088-76e9947c317a",
        "approximateArrivalTimestamp": "2012-04-23T18:25:43.511Z",
        "sequenceNumber": "49546986683135544286507457936321625675700192471156785154",
        "subsequenceNumber": ""
      }
    }
  ],
  "region": "us-east-1",
  "deliveryStreamArn": "arn:aws:kinesis:EXAMPLE",
  "invocationId": "invocationIdExample"
}
 */
class Request(@BeanProperty var records: Array[FireHoseRecord],
              @BeanProperty var region:String,
              @BeanProperty var deliveryStreamArn:String,
              @BeanProperty var invocationId:String) {
  def this() = this(null,null,null,null)
}
