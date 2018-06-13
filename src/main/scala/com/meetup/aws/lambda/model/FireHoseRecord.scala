package com.meetup.aws.lambda.model

import scala.beans.BeanProperty

class FireHoseRecord(@BeanProperty var recordId: String,
                     @BeanProperty var approximateArrivalTimestamp: Long,
                     @BeanProperty var data: String) {
  def this() = this("",0L,"")
}
