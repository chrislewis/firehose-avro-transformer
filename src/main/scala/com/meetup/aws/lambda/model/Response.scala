package com.meetup.aws.lambda.model

import scala.beans.BeanProperty

case class Response(@BeanProperty var records: java.util.List[ProcessedRecord]) {
  def this() = this(null)
}