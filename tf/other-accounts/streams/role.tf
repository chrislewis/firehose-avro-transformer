resource "aws_iam_role" "marketplace-innovation-dev" {
  name = "marketplace-innovation-dev"

#todo; this role needs more like the ability to write to s3 + invoke the lambda function - BP - 
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "firehose.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}
