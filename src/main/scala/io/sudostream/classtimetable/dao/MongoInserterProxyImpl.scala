package io.sudostream.classtimetable.dao

import java.time._

import com.mongodb.client.model.UpdateOptions
import io.sudostream.classtimetable.dao.ClassTimetableMongoDbSchema._
import io.sudostream.timetoteach.messages.systemwide.model.classes.{ClassDetails, ClassGroupsWrapper}
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.sessions.SessionOfTheDayWrapper
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.subjectdetail.SubjectDetailWrapper
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.time.ClassTimetableSchoolTimes
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.{ClassTimetable, TimeToTeachId}
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
    val classTimetableToInsertAsDocument = convertClassTimetableToDocumentWithEpoch(classTimetableToInsert)
    val observable = classTimetablesCollection.updateOne(
      BsonDocument(TIME_TO_TEACH_ID -> BsonString(classTimetableToInsert.timeToTeachId.value)),
      BsonDocument(
        "$push" -> BsonDocument(
          ALL_USER_CLASS_TIMETABLES -> classTimetableToInsertAsDocument
        )
      ),
      new UpdateOptions().upsert(true)
    )
    observable.toFuture()
  }

  private[dao] def convertClassTimetableToDocumentWithEpoch(classTimetableToConvert: ClassTimetable): Document = {
    val epoch = LocalDateTime.now.toInstant(ZoneOffset.UTC).toEpochMilli
    Document(
      EPOCH_MILLI_UTC -> BsonNumber(epoch),
      CLASS_NAME -> BsonString(classTimetableToConvert.className.value),
      CLASS_TIMETABLES_FOR_SPECIFIC_CLASS -> convertClassTimetableToDocument(classTimetableToConvert)
    )
  }

  def convertSubjectToBsonArray(subjects: List[SubjectDetailWrapper]): BsonArray = {
    val subjectDetailsAsDocuments = for {
      subjectDetailWrapper <- subjects
      subjectDetails = subjectDetailWrapper.subjectDetail

      subjectName = subjectDetails.subjectName.toString.toUpperCase
      startTimeIso8601 = subjectDetails.startTime.timeIso8601
      endTimeIso8601 = subjectDetails.endTime.timeIso8601
      additionalInfo = subjectDetails.additionalInfo.value
    } yield BsonDocument(
      SUBJECT_DETAIL_NAME -> BsonString(subjectName),
      SUBJECT_DETAIL_START_TIME -> BsonString(startTimeIso8601),
      SUBJECT_DETAIL_END_TIME -> BsonString(endTimeIso8601),
      SUBJECT_DETAIL_ADDITIONAL_INFO -> BsonString(additionalInfo)
    )

    BsonArray(subjectDetailsAsDocuments)
  }


  def convertSessionsOfTheWeekToBsonArray(allSessionsOfTheWeek: List[SessionOfTheDayWrapper]): BsonArray = {
    val allSessionsOfTheWeekAsDocuments = for {
      sessionOfTheDayWrapper <- allSessionsOfTheWeek
      sessionOfTheDay = sessionOfTheDayWrapper.sessionOfTheDay

      sessionName = sessionOfTheDay.sessionName.value
      startTimeIso8601 = sessionOfTheDay.startTime.timeIso8601
      endTimeIso8601 = sessionOfTheDay.endTime.timeIso8601
      dayOfTheWeek = sessionOfTheDay.dayOfTheWeek.toString.toUpperCase
      subjectAsBsonArray = convertSubjectToBsonArray(sessionOfTheDay.subjects)
    } yield BsonDocument(
      SESSIONS_WEEK_SESSION_NAME -> BsonString(sessionName),
      SESSIONS_WEEK_DAY_OF_THE_WEEK -> BsonString(dayOfTheWeek),
      SESSIONS_WEEK_START_TIME -> BsonString(startTimeIso8601),
      SESSIONS_WEEK_END_TIME -> BsonString(endTimeIso8601),
      SESSIONS_WEEK_SUBJECTS -> subjectAsBsonArray
    )

    BsonArray(allSessionsOfTheWeekAsDocuments)
  }

  def convertSchoolTimesToBsonArray(schoolTimes: ClassTimetableSchoolTimes): BsonArray = {
    val schoolTimeBoundariesAsDocuments = for {
      schoolSessionBoundaryWrapper <- schoolTimes.schoolSessionBoundaries
      schoolSessionBoundary = schoolSessionBoundaryWrapper.sessionBoundary

      sessionBoundaryName = schoolSessionBoundary.sessionBoundaryName.value
      startTimeIso8601 = schoolSessionBoundary.boundaryStartTime.timeIso8601
      boundaryType = schoolSessionBoundary.boundaryType.toString.toUpperCase
      sessionName = schoolSessionBoundary.sessionName match {
        case Some(theSessionName) => theSessionName.value
        case None => ""
      }
    } yield BsonDocument(
      SESSION_BOUNDARY_NAME -> BsonString(sessionBoundaryName),
      SESSION_BOUNDARY_START_TIME -> BsonString(startTimeIso8601),
      SESSION_BOUNDARY_TYPE -> BsonString(boundaryType),
      SESSION_BOUNDARY_SESSION_NAME -> BsonString(sessionName)
    )

    BsonArray(schoolTimeBoundariesAsDocuments)
  }

  private[dao] def convertClassTimetableToDocument(classTimetableToConvert: ClassTimetable): BsonDocument = {
    val sessionsOfTheWeekAsBsonArray: BsonArray = convertSessionsOfTheWeekToBsonArray(classTimetableToConvert.allSessionsOfTheWeek)
    val schoolTimesAsBsonArray: BsonArray = convertSchoolTimesToBsonArray(classTimetableToConvert.schoolTimes)
    BsonDocument(
      SCHOOL_TIMES -> schoolTimesAsBsonArray,
      ALL_SESSIONS_OF_THE_WEEK -> sessionsOfTheWeekAsBsonArray
    )
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////

  val classesCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getClassesCollection

  override def upsertClass(classDetails: ClassDetails): Future[UpdateResult] = {
    val classDetailsToInsertAsDocument = convertClassDetailsToDocumentWithEpoch(classDetails)
    val observable = classesCollection.updateOne(
      BsonDocument(ClassDetailsMongoDbSchema.CLASS_ID -> BsonString(classDetails.classId.value)),
      BsonDocument(
        "$set" -> BsonDocument(
          classDetailsToInsertAsDocument
        )
      ),
      new UpdateOptions().upsert(true)
    )
    observable.toFuture()
  }


  def convertClassGroupsToBsonArray(classGroups: List[ClassGroupsWrapper]): BsonArray = {
    val classGroupsAsDocuments = for {
      groupWrapper <- classGroups
      group = groupWrapper.group
    } yield BsonDocument(
      ClassDetailsMongoDbSchema.GROUP_ID -> BsonString(group.groupId.value),
      ClassDetailsMongoDbSchema.GROUP_NAME -> BsonString(group.groupName.value),
      ClassDetailsMongoDbSchema.GROUP_TYPE -> BsonString(group.groupType.toString.toUpperCase),
      ClassDetailsMongoDbSchema.GROUP_LEVEL -> BsonString(group.groupLevel.toString.toUpperCase)
    )

    BsonArray(classGroupsAsDocuments)
  }

  def convertTeachersWithWriteAccessToBsonArray(teacherWithWriteAccess: List[String]): BsonArray = {
    val teachersWithWriteAccessBsonArray = new BsonArray()

    for (teacherId <- teacherWithWriteAccess) {
      teachersWithWriteAccessBsonArray.add(BsonString(teacherId))
    }

    teachersWithWriteAccessBsonArray
  }

  private[dao] def convertClassDetailsToDocumentWithEpoch(classDetails: ClassDetails): Document = {
    val epoch = LocalDateTime.now.toInstant(ZoneOffset.UTC).toEpochMilli
    Document(
      ClassDetailsMongoDbSchema.EPOCH_MILLI_UTC -> BsonNumber(epoch),
      ClassDetailsMongoDbSchema.CLASS_NAME -> BsonString(classDetails.className.value),
      ClassDetailsMongoDbSchema.CLASS_GROUPS -> convertClassGroupsToBsonArray(classDetails.classGroups),
      ClassDetailsMongoDbSchema.TEACHERS_WITH_WRITE_ACCESS -> convertTeachersWithWriteAccessToBsonArray(
        classDetails.teachersWithWriteAccess
      )
    )
  }


  ///////////////////////////////////////////

  override def deleteClass(tttUserId: TimeToTeachId, classIdToDelete: String): Future[UpdateResult] = {
    val observable = classesCollection.updateOne(
      BsonDocument(ClassDetailsMongoDbSchema.CLASS_ID -> BsonString(classIdToDelete)),
      BsonDocument("$addToSet" ->
        BsonDocument(
          ClassDetailsMongoDbSchema.TEACHERS_WHO_DELETED_CLASS  -> BsonString(tttUserId.value)
        )
      ),
      new UpdateOptions().upsert(true)
    )
    observable.toFuture()
  }

}
