package com.meetup.aws.lambda.model

import scala.beans.BeanProperty


case class ProcessedRecord(@BeanProperty var recordId: String,
                           @BeanProperty var result: String,
                           @BeanProperty var data: String) {
  def this() = this(null,null,null)
}