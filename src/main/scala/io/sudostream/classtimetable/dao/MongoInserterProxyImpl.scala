package io.sudostream.classtimetable.dao

import java.time._

import com.mongodb.client.model.UpdateOptions
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.ClassTimetable
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonNumber, BsonString}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.Future

object MongoInserterProxyImpl {

  def convertListOfTuplesToMap(listOfTuples: List[(String, BsonDocument)]): Map[String, BsonArray] = {
    def mapBuilder(currentMap: Map[String, BsonArray],
                   tupleToAdd: (String, BsonDocument),
                   restOfTupleList: List[(String, BsonDocument)]
                  ): Map[String, BsonArray] = {
      if (restOfTupleList.isEmpty) addToMap(currentMap, tupleToAdd)
      else {
        val newMap: Map[String, BsonArray] = addToMap(currentMap, tupleToAdd)
        mapBuilder(newMap, restOfTupleList.head, restOfTupleList.tail)
      }
    }

    mapBuilder(Map(), listOfTuples.head, listOfTuples.tail)
  }

  private def addToMap(currentMap: Map[String, BsonArray], tupleToAdd: (String, BsonDocument)) = {
    val bsonArray = currentMap.get(tupleToAdd._1) match {
      case Some(currentValue) =>
        currentValue.add(tupleToAdd._2)
        println(s"the current val = ${currentValue.toString}")
        currentValue
      case None => BsonArray(tupleToAdd._2)
    }
    val newMap = currentMap + (tupleToAdd._1 -> bsonArray)
    newMap
  }
}

class MongoInserterProxyImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoInserterProxy {

  val classTimetablesCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getClassTimetableCollection

  override def upsertClassTimetable(classTimetableToInsert: ClassTimetable): Future[UpdateResult] = {
    val classTimetableToInsertAsDocument = convertClassTimetableToDocument(classTimetableToInsert)
    val observable = classTimetablesCollection.updateOne(
      BsonDocument("_id" -> BsonString(classTimetableToInsert.timeToTeachId)),
      BsonDocument(
        "$push" -> BsonDocument(
          "classTimetables" -> classTimetableToInsertAsDocument
        )
      ),
      new UpdateOptions().upsert(true)
    )
    observable.toFuture()
  }

  private[dao] def convertClassTimetableToDocument(classTimetableToConvert: ClassTimetable): Document = {
    val epoch = LocalDateTime.now.toInstant(ZoneOffset.UTC).toEpochMilli
    Document(
      "epoch" -> BsonNumber(epoch)
    )
  }

}
