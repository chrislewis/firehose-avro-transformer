### Deploy the lambda

Just run:

```export TARGET_BUCKET="com-meetup-prod-reporting"; export TARGET_PREFIX="flume/avro/"; sbt assembly && sls deploy --stage prod```
