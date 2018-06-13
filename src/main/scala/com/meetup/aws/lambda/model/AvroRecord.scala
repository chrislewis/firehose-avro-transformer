package com.meetup.aws.lambda.model

import scala.beans.BeanProperty

case class AvroRecord(@BeanProperty var record: String,
                      @BeanProperty var schema: String,
                      @BeanProperty var date: String ) {
  def this() = this(null,null,null)
}