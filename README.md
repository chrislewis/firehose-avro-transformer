### Deploy the lambda

Just run:

in an account other than prod:
```export ROLE_ARN="arn:aws:iam::212646169882:role/firehose-avro-transformer"; export TARGET_BUCKET="com-meetup-prod-reporting"; export TARGET_PREFIX="flume/avro/"; sbt assembly && sls deploy --stage prod```

in prod:
```export TARGET_BUCKET="com-meetup-prod-reporting"; export TARGET_PREFIX="flume/avro/"; sbt assembly && sls deploy --stage prod```