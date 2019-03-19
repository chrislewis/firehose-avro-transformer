resource "aws_iam_role" "firehose-avro-transformer" {
  max_session_duration = 43200
  description          = "a role meant to be assumed by firehose-avro-transformer running in a different account "

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS":["arn:aws:iam::856721882819:root","arn:aws:iam::388712777998:root"]
        },
        "Action": "sts:AssumeRole",
        "Condition":{}}]
}
EOF
}

resource "aws_iam_role_policy_attachment" "firehose-avro-transformer-AmazonS3FullAccess" {
  role       = "${aws_iam_role.firehose-avro-transformer.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonKinesisFirehoseFullAccess"
}


resource "aws_iam_role_policy_attachment" "firehose-avro-transformer-AmazonKinesisFirehoseFullAccess" {
  role       = "${aws_iam_role.firehose-avro-transformer.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonKinesisFirehoseFullAccess"
}