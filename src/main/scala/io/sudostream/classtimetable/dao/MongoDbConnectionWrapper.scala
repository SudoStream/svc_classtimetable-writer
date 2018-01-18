package io.sudostream.classtimetable.dao

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getClassTimetableCollection: MongoCollection[Document]

  def getClassesCollection: MongoCollection[Document]
}
