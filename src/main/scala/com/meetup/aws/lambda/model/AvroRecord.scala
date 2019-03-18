package com.meetup.aws.lambda.model

import scala.beans.BeanProperty

case class AvroRecord(@BeanProperty var record: String,
                      @BeanProperty var schema: String,
                      @BeanProperty var date: String,
                      @BeanProperty var contentType: String = "avro/binary") {
  def this() = this(null,null,null)
}//application/json