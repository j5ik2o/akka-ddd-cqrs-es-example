package com.github.j5ik2o.bank.adaptor.controller

import org.hashids.Hashids

object ControllerBase {

  implicit val hashIds: Hashids = new Hashids("salt")

}
