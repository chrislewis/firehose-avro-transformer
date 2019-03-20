resource "aws_kinesis_firehose_delivery_stream" "marketplace-innovation-dev" {
  name        = "marketplace-innovation-dev"
  destination = "extended_s3"

  extended_s3_configuration {
    role_arn   = "${aws_iam_role.marketplace-innovation-dev.arn}"
    bucket_arn = "arn:aws:s3:::com.meetup.firehose"
    prefix     = "k8s-analytics-prod/"

    processing_configuration {
      enabled = "true"

      processors {
        type = "Lambda"

        parameters {
          parameter_name  = "LambdaArn"
          parameter_value = "arn:aws:lambda:us-east-1:212646169882:function:firehose-avro-transformer-prod-transform:$LATEST"
        }
      }
    }
  }
}
