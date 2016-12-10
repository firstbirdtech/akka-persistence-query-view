package com.github.filosganga.akka.persistence

import akka.persistence.query.Offset


trait QuerySupport {

  type Queries

  def queries: Queries

  def firstOffset: Offset
}
